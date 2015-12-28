package com.elastictab.model;

import java.util.List;

public class Elasticsearch {
	String alias;
	String index;
	String type;
	int batchSize = 250;
	String query;
	String valueMapping;
	List<String> routing;
	boolean useAlias;

	public boolean isUseAlias() {
		return useAlias;
	}

	public void setUseAlias(boolean useAlias) {
		this.useAlias = useAlias;
	}

	public List<String> getRouting() {
		return routing;
	}

	public void setRouting(List<String> routing) {
		this.routing = routing;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getValueMapping() {
		return valueMapping;
	}

	public void setValueMapping(String valueMapping) {
		this.valueMapping = valueMapping;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

}
