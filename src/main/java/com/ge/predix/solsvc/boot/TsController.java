package com.ge.predix.solsvc.boot;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.ge.predix.timeseries.client.ClientFactory;
import com.ge.predix.timeseries.client.TenantContext;
import com.ge.predix.timeseries.client.TenantContextFactory;
import com.ge.predix.timeseries.model.builder.QueryBuilder;
import com.ge.predix.timeseries.model.builder.QueryTag;
import com.ge.predix.timeseries.model.builder.TimeRelation;
import com.ge.predix.timeseries.model.builder.TimeUnit;

@RestController
public class TsController {	

	@Autowired
	RestTemplate restTemplate;
	private String queryUrl = "https://time-series-store-predix.run.aws-usw02-pr.ice.predix.io/v1/datapoints";
	private String predixZoneIdHeaderName = "Predix-Zone-Id";
	private String predixZoneIdHeaderValue = "d8c789d5-f55c-4c2e-a83f-b43b88565c3b";
	private String uaaOauth = "https://02fd28a4-387e-468c-86aa-0385227f407a.predix-uaa.run.aws-usw02-pr.ice.predix.io/oauth/token";
	@RequestMapping(value = "/test", produces="application/json")
	public List test() {
		List ans = null ;
		try{
			TenantContext tenant = TenantContextFactory.createQueryTenantContextFromProvidedProperties(queryUrl, getAccessToken(), predixZoneIdHeaderName, predixZoneIdHeaderValue);
			ans = ClientFactory.queryClientForTenant(tenant).getTagNames();
			}
			catch(Exception e){
					System.out.println(e);
			}
		
		return ans;
		
	}
	@RequestMapping(value = "/chargingPointInfo", produces="application/json")
	public JSONObject getChargingPointInfo(@ModelAttribute("tag") String tag){
		JSONObject result = new JSONObject();
		JSONObject body = new JSONObject();
/*		result.put("name", "");
		result.put("isAvailable", "");
		result.put("location", "");*/
		JSONArray tags = new JSONArray();
		JSONObject tagjs = new JSONObject();
		tagjs.put("name", tag);
		tags.add(tagjs);
		body.put("tags", tags);
		System.out.println("body "+body.toJSONString());
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set("authorization",getAccessToken());
			headers.set("Predix-Zone-Id", predixZoneIdHeaderValue);
			HttpEntity<String> entity = new HttpEntity<String>(body.toJSONString(),headers);
			ResponseEntity<JSONObject> httpResult = restTemplate.exchange("https://time-series-store-predix.run.aws-usw02-pr.ice.predix.io/v1/datapoints/latest", HttpMethod.POST, entity, JSONObject.class);
			result = httpResult.getBody();
		}
		catch (Exception eek) {
			System.out.println("** Exception: "+ eek.getMessage());
		}
		
		return result;
		
	}
	
	// !!! should be moved to a security config classz
	private String getAccessToken() {
		String OAUTH_SERVER_URI = uaaOauth ;
		String grantTypeUserSecret = "?grant_type=client_credentials&client_id=" + "25-team-be"
				+ "&client_secret=" + "25team";
		// acquire access token
		HttpEntity<String> request = new HttpEntity<String>(getHeadersWithClientCredentials());
		ResponseEntity<Object> response = restTemplate.exchange(OAUTH_SERVER_URI + grantTypeUserSecret, HttpMethod.POST,
				request, Object.class);
		LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) response.getBody();
		String access_token = (String) map.get("access_token");
		return access_token;
	}

	/*
	 * Prepare HTTP Headers.
	 */
	private static HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		return headers;
	}

	/*
	 * Add HTTP Authorization header, using Basic-Authentication to send
	 * client-credentials.
	 */
	private static HttpHeaders getHeadersWithClientCredentials() {
		String plainClientCredentials = "25-team-be:25team";
		String base64ClientCredentials = new String(Base64.encodeBase64(plainClientCredentials.getBytes()));

		HttpHeaders headers = getHeaders();
		headers.add("Authorization", "Basic " + base64ClientCredentials);
		return headers;
	}
}
