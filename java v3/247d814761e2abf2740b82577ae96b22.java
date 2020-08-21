package com.tencent.cloud;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SingtrueUtil {

	private static final String TAG = "SigtrueUtil";
	private final static String ALGORITHM = "TC3-HMAC-SHA256"; // TC3-HMAC-SHA256：签名方法，目前固定取该值；
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private final static String CT_JSON = "application/json; charset=utf-8";

	/**
	 * 腾讯云签名
	 *
	 * @param bodyParam body参数
	 * @param service 服务名
	 * @param secretId  secretId
	 * @param secretKey secretKey
	 * @param tempToken 临时证书所用的 Token ，需要结合临时密钥一起使用。临时密钥和 Token
	 *                  需要到访问管理服务调用接口获取。长期密钥不需要 Token。
	 * @param action    接口名称
	 * @param version   版本号
	 * @param region    区域
	 * @return headers 请求头
	 * @throws Exception e
	 */
	public static HashMap<String, String> getAuthorizationHearder(
			String host,
			String bodyParam, 
			String service,
			String secretId, 
			String secretKey,
			String tempToken, 
			String action, 
			String version, 
			String region) throws Exception {
		String timestamp = String.valueOf(System.currentTimeMillis() / 1000);// 当前 UNIX 时间戳，可记录发起 API
		// 请求的时间。注意：如果与服务器时间相差超过5分钟，会引起签名过期错误
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		// 注意时区，否则容易出错
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String date = sdf.format(new Date(Long.valueOf(timestamp + "000")));

		// ************* 步骤 1：拼接规范请求串 *************
		String httpRequestMethod = "POST";
		String canonicalUri = "/";
		String canonicalQueryString = "";
		String canonicalHeaders = "content-type:application/json; charset=utf-8\n" + "host:" + host + "\n";
		String signedHeaders = "content-type;host";

		String hashedRequestPayload = sha256Hex(bodyParam);
		String canonicalRequest = httpRequestMethod + "\n" + canonicalUri + "\n" + canonicalQueryString + "\n"
				+ canonicalHeaders + "\n" + signedHeaders + "\n" + hashedRequestPayload;
		System.out.println(canonicalRequest);

		// ************* 步骤 2：拼接待签名字符串 *************
		String credentialScope = date + "/" + service + "/" + "tc3_request";
		String hashedCanonicalRequest = sha256Hex(canonicalRequest);
		String stringToSign = ALGORITHM + "\n" + timestamp + "\n" + credentialScope + "\n" + hashedCanonicalRequest;
		System.out.println(stringToSign);

		// ************* 步骤 3：计算签名 *************
		byte[] secretDate = hmac256(("TC3" + secretKey).getBytes(UTF8), date);
		byte[] secretService = hmac256(secretDate, service);
		byte[] secretSigning = hmac256(secretService, "tc3_request");
		String signature = bytesToHexFun(hmac256(secretSigning, stringToSign));// DatatypeConverter.printHexBinary(hmac256(secretSigning,
		// stringToSign)).toLowerCase();
		System.out.println(signature);

		// ************* 步骤 4：拼接 Authorization *************
		String authorization = ALGORITHM + " " + "Credential=" + secretId + "/" + credentialScope + ", "
				+ "SignedHeaders=" + signedHeaders + ", " + "Signature=" + signature;
		System.out.println(authorization);

		HashMap<String, String> headers = new HashMap<>();
		if (tempToken != null && !tempToken.isEmpty()) {
			headers.put("X-TC-Token", tempToken);
		}
		headers.put("Authorization", authorization);
		headers.put("Content-Type", CT_JSON);
		headers.put("Host", host);
		headers.put("X-TC-Action", action);
		headers.put("X-TC-Timestamp", timestamp);
		headers.put("X-TC-Version", version);
		headers.put("X-TC-Region", region);

        StringBuilder sb = new StringBuilder();
        sb.append("curl -X POST https://").append(host).append(" -H \"Authorization: ").append(authorization)
                .append("\"").append(" -H \"Content-Type: application/json; charset=utf-8\"").append(" -H \"Host: ")
                .append(host).append("\"").append(" -H \"X-TC-Action: ").append(action).append("\"")
                .append(" -H \"X-TC-Timestamp: ").append(timestamp).append("\"").append(" -H \"X-TC-Version: ")
                .append(version).append("\"").append(" -H \"X-TC-Region: ").append(region).append("\"").append(" -d '")
                .append(bodyParam).append("'");
        System.out.println(sb);
		return headers;
	}

	private static byte[] hmac256(byte[] key, String msg) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		SecretKeySpec secretKeySpec = new SecretKeySpec(key, mac.getAlgorithm());
		mac.init(secretKeySpec);
		return mac.doFinal(msg.getBytes(UTF8));
	}

	private static String sha256Hex(String s) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] d = md.digest(s.getBytes(UTF8));
		return bytesToHexFun(d);// DatatypeConverter.printHexBinary(d).toLowerCase();
	}

	private static String bytesToHexFun(byte[] bytes) {
		StringBuilder buf = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) { // 使用String的format方法进行转换
			buf.append(String.format("%02x", b & 0xff));
		}
		return buf.toString();
	}
}
