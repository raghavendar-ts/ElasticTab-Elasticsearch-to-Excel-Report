package com.elastictab.app;


import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

public class Test {
	public static void main(String[] args) {

		try {
			Builder builder = Settings.settingsBuilder();
			builder.put("client.transport.sniff", true);
			builder.put("cluster.name", "elasticsearch");

			Settings settings = builder.build();

			Client esClient = TransportClient.builder().settings(settings).build().addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));
			
			SearchResponse scrollResp  = esClient.prepareSearch("test")
			        .setTypes("test3")
			        .setQuery("{   \"query\":{        \"match_all\":{}            },    \"fields\":[\"name\"] }")  
			         .setScroll(new TimeValue(60000))
			        .setSize(250).addSort("name", SortOrder.ASC)
			        .execute()
			        .actionGet();
			int i=0;
			while (true) {
				System.out.println(scrollResp.getHits().totalHits());
	//System.out.println(scrollResp.getHits().getHits().length);
			    for (SearchHit hit : scrollResp.getHits().getHits()) {
//System.out.println(hit.sourceAsMap());
			    	//System.out.println(i++);

			    }
			    scrollResp = esClient.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
			    if (scrollResp.getHits().getHits().length == 0) {
			        break;
			    }
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}
}
