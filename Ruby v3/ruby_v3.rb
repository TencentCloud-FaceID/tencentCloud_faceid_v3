# -*- coding: UTF-8 -*-
# require ruby>=2.3.0
require 'digest'
require 'json'
require 'time'
require 'openssl'

# 密钥参数
secret_id = ''
secret_key = ''

service = 'faceid'
host = 'faceid.tencentcloudapi.com'
endpoint = 'https://' + host
region = 'ap-guangzhou'
action = 'DetectAuth'
version = '2018-03-01'
algorithm = 'TC3-HMAC-SHA256'
timestamp = Time.now.to_i
# timestamp = 1551113065
date = Time.at(timestamp).utc.strftime('%Y-%m-%d')

# ************* 步骤 1：拼接规范请求串 *************
http_request_method = 'POST'
canonical_uri = '/'
canonical_querystring = ''
canonical_headers = "content-type:application/json; charset=utf-8\nhost:#{host}\n"
signed_headers = 'content-type;host'
# params = { 'Limit' => 1, 'Filters' => [{ 'Name' => 'instance-name', 'Values' => ['未命名'] }] }
# payload = JSON.generate(params, { 'ascii_only' => true, 'space' => ' ' })
# json will generate in random order, to get specified result in *******, we hard-code it here.
payload = '{"RuleId": "0"}'
hashed_request_payload = Digest::SHA256.hexdigest(payload)
canonical_request = [
                        http_request_method,
                        canonical_uri,
                        canonical_querystring,
                        canonical_headers,
                        signed_headers,
                        hashed_request_payload,
                    ].join("\n")

puts canonical_request

# ************* 步骤 2：拼接待签名字符串 *************
credential_scope = date + '/' + service + '/' + 'tc3_request'
hashed_request_payload = Digest::SHA256.hexdigest(canonical_request)
string_to_sign = [
                    algorithm,
                    timestamp.to_s,
                    credential_scope,
                    hashed_request_payload,
                 ].join("\n")
puts string_to_sign

# ************* 步骤 3：计算签名 *************
digest = OpenSSL::Digest.new('sha256')
secret_date = OpenSSL::HMAC.digest(digest, 'TC3' + secret_key, date)
secret_service = OpenSSL::HMAC.digest(digest, secret_date, service)
secret_signing = OpenSSL::HMAC.digest(digest, secret_service, 'tc3_request')
signature = OpenSSL::HMAC.hexdigest(digest, secret_signing, string_to_sign)
puts signature

# ************* 步骤 4：拼接 Authorization *************
authorization = "#{algorithm} Credential=#{secret_id}/#{credential_scope}, SignedHeaders=#{signed_headers}, Signature=#{signature}"
puts authorization

puts  'curl -X POST ' + endpoint \
      + ' -H "Authorization: ' + authorization + '"' \
      + ' -H "Content-Type: application/json; charset=utf-8"' \
      + ' -H "Host: ' + host + '"' \
      + ' -H "X-TC-Action: ' + action + '"' \
      + ' -H "X-TC-Timestamp: ' + timestamp.to_s + '"' \
      + ' -H "X-TC-Version: ' + version + '"' \
      + ' -H "X-TC-Region: ' + region + '"' \
      + " -d '" + payload + "'"