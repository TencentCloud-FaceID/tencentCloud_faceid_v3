package com.tencent.cloudtest;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.SigtrueUtil;

public class TestSingtrue {
	
	private static String urlStr = "https://faceid.tencentcloudapi.com";
	private static String bodyParam = "{\"RuleId\":\"3\"}";
	private static String service = "faceid";
	private static String version = "2018-03-01";
	private static String region = "ap-guangzhou";
	private static String action = "DetectAuth";
	private static String tempToken = null;
	private static String secretId = "";
	private static String secretKey = "";
	private final static String host = "faceid.tencentcloudapi.com";
	
	public static void main(String[] args) {
		try {
			HashMap<String, String> headerHashMap = SigtrueUtil.getAuthorizationHearder(host,bodyParam, service, secretId, secretKey, tempToken, action, version,region);
			String respString = getBizTokenKey(headerHashMap);
			System.out.println(respString);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String getBizTokenKey(HashMap<String, String> headerMap) throws IOException {
		String resultString;
		HttpURLConnection httpURLConnection = null;
		try {
			URL url = new URL(urlStr);
			httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setConnectTimeout(6000);
			httpURLConnection.setUseCaches(false);// 不使用缓存
			httpURLConnection.setInstanceFollowRedirects(true);// 是成员变量 仅作用域当前函数，设置当前这个对象
			httpURLConnection.setReadTimeout(3000);
			httpURLConnection.setDoInput(true);
			httpURLConnection.setDoOutput(true);
			httpURLConnection.setRequestMethod("POST");
			for (Map.Entry<String, String> entry : headerMap.entrySet()) {
				httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
			}
			// httpURLConnection.setRequestProperty();
			httpURLConnection.connect();
			// ---------------使用字节流发送数据---------------------------
			OutputStream out = httpURLConnection.getOutputStream();
			// 缓冲字节流 包装字节流
			BufferedOutputStream bos = new BufferedOutputStream(out);
			// 把字节流数组写入缓冲区中
			bos.write(bodyParam.getBytes("UTF-8"));
			// 刷新缓冲区 发送数据
			bos.flush();
			out.close();
			bos.close();
			// 如果响应码为200代表请求访问成功
			if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				InputStream in = httpURLConnection.getInputStream();
				resultString = getContent(in);
			} else {
				throw new RuntimeException("请求失败");
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("请求失败");
		} finally {
			if (httpURLConnection!= null) {
				httpURLConnection.disconnect();
			}
		}
		return resultString;
	}
	
	/**
     * inputStream转为String类型
     *
     * @param inputStream
     * @return
     */
    private static String getContent(InputStream inputStream) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line + "/n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString().replace("/n", "");
    }
}
