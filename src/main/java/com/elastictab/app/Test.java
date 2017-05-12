package com.elastictab.app;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import com.carrotsearch.hppc.ObjectLookupContainer;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.elastictab.report.ESReport;

public class Test {
	public static void main(String[] args) {

		try {
			Builder builder = Settings.builder();
			//builder.put("client.transport.sniff", true);
			builder.put("cluster.name", "elasticsearch");

			Settings settings = builder.build();
			
			String stringQuery = "{\"match_all\" : {  }}";
			String q = QueryParser.escape(stringQuery);
			System.out.println(q);
			//System.out.println(new MatchAllQueryBuilder());
			Client esClient = new PreBuiltTransportClient(settings).addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));
			
			
					
			
			QueryBuilder  queryBuilder= QueryBuilders.wrapperQuery(stringQuery);
			
			String[] fields = new String[2];
			fields[0]="name";
			fields[1]="name1";
			SearchResponse scrollResp  = esClient.prepareSearch("test")
			        .setTypes("test")			        
			        .setQuery(queryBuilder).setFetchSource(fields, null)
			        .setSize(250)//.addSort("name", SortOrder.ASC)
			        .get();
			
			SearchHit[] searchHits = scrollResp.getHits().getHits();
			for(SearchHit searchHit:searchHits){
				System.out.println(searchHit.getSourceAsMap());
			}
			
			/*int i=0;
			while (true) {
				System.out.println(scrollResp);
				System.out.println(scrollResp.getHits().totalHits());
	//System.out.println(scrollResp.getHits().getHits().length);
			    //for (SearchHit hit : scrollResp.getHits().getHits()) {
//System.out.println(hit.sourceAsMap());
			    	//System.out.println(i++);

			   // }
			    scrollResp = esClient.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
			    if (scrollResp.getHits().getHits().length == 0) {
			        break;
			    }
			}*/

		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

	}
}
