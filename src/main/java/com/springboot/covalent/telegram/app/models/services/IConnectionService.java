package com.springboot.covalent.telegram.app.models.services;

import java.util.HashMap;

public interface IConnectionService {

	public String ConectarConURL(String stringURL, HashMap<String, String> bodyPOSTJson) throws Exception;
}
