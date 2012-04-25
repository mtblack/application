/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// XXX unentangle UI and SVC parts
public class Field implements FieldSet {
	private static final Logger log = LoggerFactory.getLogger(Field.class);
	protected SchemaUtils utils = new SchemaUtils();
	
	private FieldParent parent;

	private Map<String, Instance> instances = new HashMap<String, Instance>();

	/* UI */
	private Boolean seperate_default = false;
	private Stack<String> merged = new Stack<String>();
	private Map<String, Option> options = new HashMap<String, Option>();
	private List<Option> options_list = new ArrayList<Option>();

	/* Services */

	public Field(FieldParent record, String id) {
		this.parent = record;
		//String parentid = record.getID();
		String parentid = this.getRecord().getID();
		if (record instanceof Repeat){
			if(((Repeat) record).getSearchType().equals("repeator")) {
				parentid = parent.getParent().getID();
				this.parent = parent.getParent();
			}
			else if(((Repeat) record).children.size() == 1){
				parentid = parent.getParent().getID();
			}
		}
		utils.setString("parentID", parentid);
		this.initialiseVariables(null,id);
	}
	public Field(FieldParent record, ReadOnlySection section) {
		this.parent = record;
		String parentid = this.getRecord().getID();
		if (record instanceof Repeat){
			if(((Repeat) record).getSearchType().equals("repeator")) {
				parentid = parent.getParent().getID();
				this.parent = parent.getParent();
			}
		}
		utils.setString("parentID", parentid);
		this.initialiseVariables(section,null);
	}
	/*
	public Field(FieldParent record, String id) {
		this.parent = record;
		utils.setString("parentID", record.getRecord().getID());
		this.initialiseVariables(null,id);
	}
	public Field(FieldParent record, ReadOnlySection section) {
		this.parent = record;
		utils.setString("parentID", record.getRecord().getID());
		this.initialiseVariables(section,null);
	}
	*/
	protected void initialiseVariables(ReadOnlySection section, String tempid) {

		this.setRepeatSubRecord(false);
		utils.initStrings(section,"@id",tempid);
		
		utils.initSet(section,"@autocomplete",new String[] { "" });
		utils.initBoolean(section,"@container",false);
		utils.initBoolean(section,"@xxx_ui_refactored",true);

		utils.initStrings(section,"selectorID", utils.getString("parentID"));
		utils.initStrings(section,"selector", utils.getString("selectorID") + "-" +  utils.getString("@id"));
		utils.initStrings(section,"preselector", ".csc-" );
		utils.initStrings(section,"decoratorselector", "cs-" );
		
		utils.initStrings(section, "@primarykey", utils.getString("selector"));
		//utils.initStrings(section,"label", "" + utils.getString("selector") + "Label");
		//this should be overwritten by selector but currently the UI has inconsistencies which means teh labels are always defined correctly inspite of the selector
		utils.initStrings(section,"label", "" + utils.getString("selectorID") + "-" +  utils.getString("@id") + "Label");

		utils.initStrings(section,"@userecord", "");
		utils.initStrings(section,"@onlyifexists","");
		utils.initStrings(section,"@selector-affix", "");
		utils.initStrings(section,"@label-affix", "-label");
		utils.initStrings(section,"@serviceurl", null);
		utils.initStrings(section,"@ui-spec-prefix","");
		utils.initBoolean(section, "@ui-readonly", false);
		utils.initBoolean(section,"@ui-spec-inherit",false);
		utils.initStrings(section,"@with-csid","");

		utils.initStrings(section,"@ui-func", "");
		utils.initStrings(section,"@ui-args", "");
		utils.initStrings(section,"@ui-type", "plain");
		utils.initStrings(section,"@ui-search", "");
		if (utils.getString("@ui-type").equals("date")) {
			seperate_default = true;
		}
		if(utils.getString("@ui-type").startsWith("groupfield")){
			String parts[] = utils.getString("@ui-type").split("/");
			this.parent.getRecord().addNestedFieldList(parts[1]);
		}
		
		utils.initBoolean(section,"@seperate_ui_container",seperate_default);
		utils.initStrings(section,"use-csid",null);
		utils.initStrings(section,"use-csid/@id",null);
		

		utils.initStrings(section,"autocomplete-selector", utils.getString("selector") + "-autocomplete");
		utils.initStrings(section,"autocomplete-options/funcName", "");
		utils.initStrings(section,"autocomplete-options/strings", "");
		utils.initStrings(section,"container-selector", utils.getString("selector") + "-container");
		utils.initStrings(section,"precontainer-selector", utils.getString("preselector"));
		utils.initStrings(section,"pretitle-selector", utils.getString("preselector"));
		utils.initStrings(section,"title-selector", utils.getString("selector") + "-titlebar");
		// used by uispec to create new structure
		utils.initBoolean(section,"@as-expander",false);
		utils.initBoolean(section,"@as-conditional-expander",false);
		utils.initBoolean(section,"@in-trueTree", false);
		
		
		utils.initBoolean(section,"@in-title",false);
		// no longer needed/used
		// in_tab = Util.getBooleanOrDefault(section, "/@in-tab", false);
		
		utils.initStrings(section,"services-tag", utils.getString("@id"));

		Set<String> minis = Util.getSetOrDefault(section, "/@mini",
				new String[] { "" });
		if (minis.contains("number")) {
			this.parent.getRecord().setMiniNumber(this);
		}
		if (minis.contains("summary")) {
			this.parent.getRecord().setMiniSummary(this);
		}
		if (minis.contains("list")) {
			this.parent.getRecord().addMiniSummaryList(this);
		}
		for (String s : minis) {
			this.parent.getRecord().addMiniDataSet(this, s);
		}

		utils.initBoolean(section,"@display-name",false);
		if (utils.getBoolean("@display-name"))
			this.parent.getRecord().setDisplayName(this);
		
		utils.initBoolean(section,"@exists-in-services",true);

		utils.initSet(section,"default",new String[] { "" });
		utils.initSet(section,"enum/default",new String[] { "" });		
		utils.initBoolean(section,"enum/@has-blank",true);
		utils.initStrings(section,"enum/blank-value", this.parent.enumBlankValue());
		utils.initSet(section,"@default",new String[] { "" });
		utils.initStrings(section,"@section", "common");
		utils.initStrings(section,"services-filter-param", null);
		if (utils.getString("services-filter-param") != null)
			this.parent.getRecord().setServicesFilterParam(utils.getString("services-filter-param"),this);
		
		utils.initStrings(section,"@datatype", "");
		
		utils.initSet(section,"@attributes",new String[] {"GET","PUT","POST","DELETE"});
		
		
		if (utils.getSet("@autocomplete").size() > 0) {
			for (String autocomplete_instance_id : utils.getSet("@autocomplete")) {
				autocomplete_instance_id = autocomplete_instance_id.trim();
				Record r = this.parent.getRecord();
				r.getSpec().addTermlist(autocomplete_instance_id, this);
			}
		}
		
	}


	public String getID() {
		return  utils.getString("@id");
	}

	public SchemaUtils getUtils() {
		return utils;
	}
	public String getAutocompleteSelector() {
		return utils.getString("autocomplete-selector");
	}
	public String getAutocompleteFuncName() {
		return utils.getString("autocomplete-options/funcName");
	}
	public String getAutocompleteStrings() {
		return utils.getString("autocomplete-options/strings");
	}

	public String getContainerSelector() {
		return utils.getString("container-selector");
	}
	public String getPreContainerSelector() {
		return utils.getString("precontainer-selector");
	}

	public String getSelector() {
		return utils.getString("selector");
	}
	public String getPreSelector() {
		return utils.getString("preselector");
	}
	public String getDecoratorSelector() {
		return utils.getString("decoratorselector");
	}
	public String getLabel() {
		return utils.getString("label");
	}
	public String getUIprefix(){
		return getPreSelector() + utils.getString("parentID") + "-";
	}
	public String getUILabelSelector(String id){
		return getUIprefix() +  id + "-label";
	}
	public String getUILabelSelector() {
		return getUIprefix() +  utils.getString("@id") + "-label";
	}

	public String getUIType() {
		return utils.getString("@ui-type");
	}
	public String getSearchType() {
		return utils.getString("@ui-search");
	}
	public void setSearchType(String val) {
		utils.setString("@ui-search",val);
	}
	
	public String getUIFunc() {
		return utils.getString("@ui-func");
	}

	public String getUIArgs() {
		return utils.getString("@ui-args");
	}
	
	public Boolean isInTitle() {
		return utils.getBoolean("@in-title");
	}

	// public boolean isInTab() { return in_tab; }
	public Boolean hasContainer() {
		return utils.getBoolean("@container");
	}

	public Boolean isInServices() {
		return utils.getBoolean("@exists-in-services");
	}

	public boolean isExpander() {
		return utils.getBoolean("@as-expander");
	}
	public boolean isInTrueTree(){
		return utils.getBoolean("@in-trueTree");
	}
	public boolean isConditionExpander(){
		return utils.getBoolean("@as-conditional-expander");
	}
	public boolean isReadOnly(){
		return utils.getBoolean("@ui-readonly");
	}
	
	public boolean isRepeatSubRecord() {
		return utils.getBoolean("@is-subrecord");
	}

	public String useCsid() {
		return utils.getString("use-csid");
	}
	public String useCsidField() {
		return utils.getString("use-csid/@id");
	}
	
	public void setRepeatSubRecord(Boolean var) {
		utils.setBoolean("@is-subrecord",var);
	}

	public Boolean isRefactored() {
		return !utils.getBoolean("@seperate_ui_container");
	} // used until UI layer has moved all autocomplete to one container view

	public String getTitleSelector() {
		return utils.getString("title-selector");
	}
	
	public String getPreTitleSelector() {
		return utils.getString("pretitle-selector");
	}
	
	//used in generateGroupField in uispec for elpaths
	public String getPrimaryKey() {
		return utils.getString("@primarykey");
	}

	public String getServicesFilterParam() {
		return utils.getString("services-filter-param");
	}

	public String getServicesTag() {
		return utils.getString("services-tag");
	}
	
	public String getServicesUrl(){
		return utils.getString("@serviceurl");
	}
	
	//XXX could be used for validation at the app layer
	public String getDataType(){
		return utils.getString("@datatype");
	}

	void setType(String in) {
		utils.setString("@ui-type",in);
	}

	// CSPACE-869
	void addMerge(String mid, String rank) {
		try {
			int index = Integer.parseInt(rank);
			if (merged.size() < index) {
				merged.setSize(index);
			}
			merged.add(index, mid);
			parent.getRecord().setMerged(this);
		} catch (Exception e) {
			// something wrong - could have been a non number
			log.error("Failed to add Merge field " +  utils.getString("@id") + " into field "
					+  utils.getString("@id") + " at rank " + rank);
		}

	}

	public String getMerge(int index) {
		return merged.get(index);
	}

	public List<String> getAllMerge() {
		return merged;
	}

	public Boolean hasMergeData() {
		if (merged.isEmpty())
			return false;

		return true;
	}

	public String getLabelAffix() {
		return utils.getString("@label-affix");
	}
	public String getSelectorAffix() {
		return utils.getString("@selector-affix");
	}

	public String getUISpecPrefix() {
		return utils.getString("@ui-spec-prefix");
	}
	public Boolean getUISpecInherit() {
		return utils.getBoolean("@ui-spec-inherit");
	}

	public String getWithCSID() {
		return utils.getString("@with-csid");
	}
	
	public Boolean usesRecord() {
		if (utils.getString("@userecord") != null && !utils.getString("@userecord").equals("")) {
			return true;
		}
		return false;
	}
	
	public String usesRecordValidator() {
		if(utils.getString("@onlyifexists") !=null && !utils.getString("@onlyifexists").equals("")){
			return utils.getString("@onlyifexists");
		}
		return null;
	}
	
	public Record usesRecordId() {
		if (usesRecord()) {
			return this.getRecord().getSpec().getRecord(utils.getString("@userecord"));
		}
		return null;
	}

	void addOption(String id, String name, String sample, boolean dfault) {
		Option opt = new Option(id, name, sample);
		if (dfault) {
			opt.setDefault();
			utils.getSet("@default").add(opt.getID());
		}
		options.put(id, opt);
		options_list.add(opt);
		if ("plain".equals(utils.getString("@ui-type")))
			setType("dropdown");
	}

	public Option getOption(String id) {
		return options.get(id);
	}

	public Option[] getAllOptions() {
		return options_list.toArray(new Option[0]);
	}
	
	//getAllFieldPerms now getAllFieldOperations
	public String[] getAllFieldOperations(){
		return utils.getSet("@attributes").toArray(new String[0]);
	}

	public boolean hasFieldPerm(String perm){
		return utils.getSet("@attributes").contains(perm);
	}
	
	public String getOptionDefault() {
		utils.getSet("@default").remove("");
		return StringUtils.join(utils.getSet("@default"), ",");
	}

	public FieldParent getParent() {
		return this.parent;
	}

	public boolean hasServicesParent(){
		return false;
	}
	public String[] getServicesParent() {
		return new String[0];
	}
	public void setParent(FieldParent fp) {
		this.parent = fp;
	}

	public Record getRecord() {
		return parent.getRecord();
	}

	public String[] getIDPath() {
		if (parent instanceof Repeat && !((Repeat) parent).getSearchType().equals("repeator")) {
			String[] pre = ((Repeat) parent).getIDPath();
			String[] out = new String[pre.length + 1];
			System.arraycopy(pre, 0, out, 0, pre.length);
			out[pre.length] =  utils.getString("@id");
			return out;
		} else {
			return new String[] {  utils.getString("@id") };
		}
	}

	public String getSection() { //
		return utils.getString("@section");
	}

	public Instance[] getAllAutocompleteInstances() {
		return instances.values().toArray(new Instance[0]);
	}

	// XXX hack so just returns the first autocomplete instance if multiple
	// assigned
	public Instance getAutocompleteInstance() {
		if (hasAutocompleteInstance()) {
			return getAllAutocompleteInstances()[0];
		}
		return null;
	}

	public Boolean hasAutocompleteInstance() {
		if (utils.getSet("@autocomplete").size() > 0) {
			for (String autocomplete_instance_id : utils.getSet("@autocomplete")) {
				autocomplete_instance_id = autocomplete_instance_id.trim();
				if (!autocomplete_instance_id.equals("")) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasEnumBlank() {
		return utils.getBoolean("enum/@has-blank");
	}

	public String enumBlankValue() {
		return utils.getString("enum/blank-value");
	}

	public String getEnumDefault() {
		return StringUtils.join(utils.getSet("enum/default"), ",");
	}
	public String getDefault() {
		return StringUtils.join(utils.getSet("default"), ",");
	}

	public boolean hasDefault() {
		if (utils.getSet("default").isEmpty() || ( utils.getSet("default").size()==1 && utils.getSet("default").contains(""))) {
			return false;
		}
		return true;
	}
	public boolean isDefault(String name) {
		if (utils.getSet("default").contains(name)) {
			return true;
		}
		return false;
	}
	public boolean isEnumDefault(String name) {
		if (utils.getSet("enum/default").contains(name)) {
			return true;
		}
		return false;
	}

	public void config_finish(Spec spec) {
		if (utils.getSet("@autocomplete").size() > 0) {
			for (String autocomplete_instance_id : utils.getSet("@autocomplete")) {
				autocomplete_instance_id = autocomplete_instance_id.trim();
				if (!autocomplete_instance_id.equals("")) {
					instances.put(autocomplete_instance_id, spec
							.getInstance(autocomplete_instance_id));
				}
			}
		}
	}
	
	void dump(StringBuffer out) {
		out.append("  id=" + this.getID() + "\n");
		out.append("    type=" + utils.getSet("@type") + "\n");
		utils.dump(out);
	}

	void dumpJson(JSONObject out) throws JSONException {
		JSONObject record = new JSONObject();
		utils.dumpJson(record, "attributes");
		record.put("id", this.getID());
		record.put("type", utils.getSet("@type"));
		out.put(this.getID(), record);
	}
	

}