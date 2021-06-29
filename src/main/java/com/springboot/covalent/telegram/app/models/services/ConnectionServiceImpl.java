package com.springboot.covalent.telegram.app.models.services;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class ConnectionServiceImpl implements IConnectionService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	public static final MediaType XML = MediaType.parse("application/xml; charset=utf-8");

	@Override
	public String ConectarConURL(String stringURL, HashMap<String, String> bodyPOSTJson) throws Exception {

		OkHttpClient okHttpClient = new OkHttpClient.Builder()
				.connectTimeout(600, TimeUnit.SECONDS).readTimeout(600, TimeUnit.SECONDS).build();

		try {

			Request requestOKHTTP = null;

			if (bodyPOSTJson != null) {

				String json = new ObjectMapper().writeValueAsString(bodyPOSTJson);
				RequestBody body = RequestBody.create(JSON, json);
				requestOKHTTP = new Request.Builder().url(stringURL).post(body).build();

			} else {

				requestOKHTTP = new Request.Builder().url(stringURL).build();

			}

			Response response = okHttpClient.newCall(requestOKHTTP).execute();

			String bodyString = response.body().string();

			log.debug(bodyString);

			return bodyString;

		} catch (Exception e) {

			throw e;
		}

	}

}
