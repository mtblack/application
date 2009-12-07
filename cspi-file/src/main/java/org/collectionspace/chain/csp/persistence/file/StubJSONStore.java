/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.file;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.json.JSONException;
import org.json.JSONObject;

// XXX ids should be a separate service, not mixed in here: currently lacking the delegation and priority system
/** An implementation of Storage which uses the filesystem.
 * 
 * We require exactly one slash: the storage type. If the prefix doesn't exist, we create it.
 */
public class StubJSONStore implements Storage {
	private String store_root;
	private IdGenerator id;
	private static final Random rnd=new Random();

	/** Generate a file from a path.
	 * 
	 * @param path the path
	 * @return the file
	 * @throws UnderlyingStorageException 
	 * @throws ExistException 
	 */
	private File fileFromPath(String path) throws UnderlyingStorageException, ExistException {
		if(path.startsWith("/"))
			path=path.substring(1);
		String[] parts=path.split("/");
		if(parts.length==1)
			throw new ExistException("Cannot store in root directory");
		for(int i=0;i<2;i++)
			parts[i]=parts[i].replaceAll("[^A-Za-z0-9_,.-]","");
		File root=new File(store_root,parts[0]);
		if(!root.exists()) {
			root.mkdir();
		}
		return new File(root,parts[1]+".json");
	}

	private File dirFromPath(String path) {
		if(path.startsWith("/"))
			path=path.substring(1);
		if(path.endsWith("/"))
			path=path.substring(path.length()-1);
		path=path.replaceAll("[^A-Za-z0-9_,.-]","");
		File root=new File(store_root,path);
		if(!root.exists()) {
			root.mkdir();
		}
		return new File(store_root,path);
	}

	public String getStoreRoot() { return store_root; }

	/** Create stub store based on filesystem
	 * 
	 * @param store_root path to store
	 */
	public StubJSONStore(String store_root) {
		this.store_root=store_root;
		id=new IdGenerator(store_root);
	}

	private boolean idRequest(String path) {
		if(path.startsWith("/"))
			path=path.substring(1);
		String[] parts=path.split("/");
		if(parts.length!=2)
			return false; // Error will be thrown in due course
		return "id".equals(parts[0]);
	}
	
	/* (non-Javadoc)
	 * @see org.collectionspace.JSONStore#retrieveJson(java.lang.String)
	 */
	public JSONObject retrieveJSON(String filePath) throws ExistException, UnderlyingStorageException, UnimplementedException {
		if(idRequest(filePath))
			return id.retrieveJSON(filePath);
		File jsonFile = fileFromPath(filePath);
		if (!jsonFile.exists()) {
			throw new ExistException("No such file: " + filePath);
		}
		try {
			FileReader r=new FileReader(jsonFile);
			String data=IOUtils.toString(r);
			r.close();
			JSONObject jsonObject = new JSONObject(data);
			return jsonObject;
		}
		catch (IOException ioe) {
			return new JSONObject(); // XXX
		}
		catch (JSONException je) {
			return new JSONObject(); // XXX
		}
	}

	/* (non-Javadoc)
	 * @see org.collectionspace.JSONStore#storeJson(java.lang.String, org.json.JSONObject)
	 */
	public void createJSON(String filePath, JSONObject jsonObject) throws ExistException, UnderlyingStorageException, UnimplementedException {
		if(idRequest(filePath))
			id.createJSON(filePath,jsonObject);
		set(filePath,jsonObject,true);
	}

	public void updateJSON(String filePath, JSONObject jsonObject) throws ExistException, UnderlyingStorageException, UnimplementedException {
		if(idRequest(filePath))
			id.updateJSON(filePath,jsonObject);
		set(filePath,jsonObject,false);
	}

	private synchronized void set(String filePath, JSONObject jsonObject,boolean create) throws ExistException, UnderlyingStorageException {
		System.out.println("file path:" + filePath);
		File jsonFile = fileFromPath(filePath);
		if(create && jsonFile.exists())
			throw new ExistException("Cannot create: already exists");
		if(!create && !jsonFile.exists())
			throw new ExistException("Cannot update: does not exist");
		try {
			FileWriter w=new FileWriter(jsonFile);
			IOUtils.write(jsonObject.toString(),w);
			w.close();
		} catch (IOException ioe) {
			return;
		}
	}

	public String[] getPaths(String subdir) throws ExistException, UnimplementedException, UnderlyingStorageException {
		if("id".equals(subdir))
			return id.getPaths("");
		File dir=dirFromPath(subdir);
		if(!dir.isDirectory())
			return new String[]{};
		List<String> out=new ArrayList<String>();
		if("".equals(subdir) || "/".equals(subdir)) {
			// Directory request
			for(File f : dir.listFiles())
				out.add(f.getName());
		} else {
			// File request
			for(String f : dir.list()) {
				if(f.endsWith(".json")) {
					f=f.substring(0,f.length()-5);
					out.add(f);
				}
			}
		}
		return out.toArray(new String[0]);
	}

	public String autocreateJSON(String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		if("id".equals(filePath))
			id.autocreateJSON("",jsonObject);
		while(true) {
			int tail=rnd.nextInt(Integer.MAX_VALUE);
			String filename=filePath+"/"+tail;
			try {
				set(filename,jsonObject,true);
				return Integer.toString(tail);
			} catch(ExistException e) {
				// Try again
			}
		}
	}

	public void deleteJSON(String filePath) throws ExistException, UnimplementedException, UnderlyingStorageException {
		if(idRequest(filePath))
			id.deleteJSON(filePath);
		File jsonFile = fileFromPath(filePath);
		if (!jsonFile.exists()) {
			throw new ExistException("No such file: " + filePath);
		}
		jsonFile.delete();
	}
}
