package com.springboot.covalent.telegram.app.controllers;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.covalent.telegram.app.models.services.IConnectionService;

@RestController
@RequestMapping
public class CovalentRestController {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	IConnectionService connectionService;

	String covalentAPIkey = "ckey_xxxxxxxxxxxxxxxxxxxxxxxxxx";
	String covalent01botTelegramAPIkey = "xxxxxxxxxxxxxxxxxxxxxxxxx";

	@PostMapping
	public HashMap<String, String> generateResponse(@RequestBody Map<String, Object> payload) {

		log.info("POST received: [[" + payload + "]]");

		Map<String, Object> message = (Map<String, Object>) payload.get("message");
		Map<String, Object> chat = (Map<String, Object>) message.get("chat");
		log.info("message: [[" + message + "]]");
		log.info("chat: [[" + chat + "]]");

		String textMessage = message.get("text") + "";

		String contractTickerSymbol = "";
		String address = "";
		String chainId = "";

		if (textMessage.toUpperCase().contains("SALDO") || textMessage.toUpperCase().contains("BALANCE")) {

			if (textMessage.contains("ETH")) {
				contractTickerSymbol = "ETH";
				chainId = "1";
			} else if (textMessage.contains("USDT")) {
				contractTickerSymbol = "USDT";
				chainId = "1";
			} else if (textMessage.contains("UNI")) {
				contractTickerSymbol = "UNI";
				chainId = "1";
			}

			String[] textMessageWords = textMessage.split(" ");
			for (int i = 0; i < textMessageWords.length; i++) {
				if (textMessageWords[i].length() > 15) {
					address = textMessageWords[i];
					break;
				}
			}

		}

		String text = "";

		log.info("contractTickerSymbol: " + contractTickerSymbol + " address: " + address + " chainId: " + chainId);

		if (!"".equals(contractTickerSymbol) && !"".equals(address) && !"".equals(chainId)) {

			String url = "https://api.covalenthq.com/v1/" + chainId + "/address/" + address + "/balances_v2/" + "?key="
					+ covalentAPIkey;

			// Enviar consulta a covalent
			try {
				String respuesta = connectionService.ConectarConURL(url, null);
				log.info("mensaje enviado con exito a: " + url);
				log.info("respuesta recibida: " + respuesta);

				ObjectMapper mapper = new ObjectMapper();
				JsonNode jsonNodeRoot = mapper.readTree(respuesta);
				JsonNode jsonNodeError = jsonNodeRoot.get("error");
				if (jsonNodeError.asBoolean()) {
					JsonNode jsonNodeErrorMessage = jsonNodeRoot.get("error_message");
					if (jsonNodeErrorMessage != null) {
						throw new Exception(
								"There is an error in the Covalent response: " + jsonNodeErrorMessage.asText());
					} else {
						throw new Exception("There is an error in the Covalent response");
					}
				}

				JsonNode jsonNodeData = jsonNodeRoot.get("data");

				if (jsonNodeData != null) {

					String quote_currency = "";
					JsonNode jsonNodeQuoteCurrency = jsonNodeData.get("quote_currency");
					if (jsonNodeQuoteCurrency != null) {
						quote_currency = jsonNodeQuoteCurrency.asText();
					}

					JsonNode jsonNodeItems = jsonNodeData.get("items");

					if (jsonNodeItems != null && jsonNodeItems.isArray()) {

						for (int i = 0; i < jsonNodeItems.size(); i++) {

							JsonNode jsonNodeItem = jsonNodeItems.get(i);

							String contract_ticker_symbol = "";
							JsonNode jsonNodeItemContractTickerSymbol = jsonNodeItem.get("contract_ticker_symbol");
							if (jsonNodeItemContractTickerSymbol != null) {
								contract_ticker_symbol = jsonNodeItemContractTickerSymbol.asText();
								if (!contractTickerSymbol.equals(contract_ticker_symbol)) {
									continue;
								}
							}

							int contract_decimals = -1;
							JsonNode jsonNodeItemContract_decimals = jsonNodeItem.get("contract_decimals");
							if (jsonNodeItemContract_decimals != null) {
								contract_decimals = jsonNodeItemContract_decimals.asInt();
							}

							String balance = "";
							JsonNode jsonNodeItemBalance = jsonNodeItem.get("balance");
							if (jsonNodeItemBalance != null) {
								balance = jsonNodeItemBalance.asText();
							}

							double quote = -1;
							JsonNode jsonNodeItemQuote = jsonNodeItem.get("quote");
							if (jsonNodeItemQuote != null) {
								quote = jsonNodeItemQuote.asDouble();
							}

							double calculatedBalance = Double.valueOf(balance) / (Math.pow(10, contract_decimals));

							text = "balance: " + calculatedBalance + " " + contract_ticker_symbol + " -> " + quote + " "
									+ quote_currency + " for address " + address;

							log.info("balance: " + balance + " decimals : " + contract_decimals + " calculateBalance: "
									+ calculatedBalance + " " + contract_ticker_symbol + " -> " + quote + " "
									+ quote_currency);

							break;
						}

						if ("".equals(text)) {
							text = "There is no " + contractTickerSymbol + " in wallet " + address;
						}

					} else {

						throw new Exception("There is no items in Covalent");

					}

				} else {

					throw new Exception("There is no data in Covalent");
				}

			} catch (Exception e) {

				// e.printStackTrace();
				log.error("error enviando/procesando mensaje de covalent:" + e.getMessage());
				text = "Sorry, " + e.getMessage();
			}

		} else {

			text = "Sorry, I don't understand you. Write 'balance + symbol + address'. Only ETH, USDT and UNI in the Ethereum network are compatible at the moment in this bot.";

		}

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("text", text);
		map.put("chat_id", String.valueOf(chat.get("id")));

		// Enviar mensaje de vuelta a telegram
		try {
			String respuesta = connectionService
					.ConectarConURL("https://api.telegram.org/bot" + covalent01botTelegramAPIkey + "/sendMessage", map);
			log.info("mensaje enviado con exito: " + map);
			log.info("respuesta recibida: " + respuesta);

		} catch (Exception e) {

			// e.printStackTrace();
			log.error("error enviando mensaje:" + e.getMessage());
		}

		return map;

	}

}
