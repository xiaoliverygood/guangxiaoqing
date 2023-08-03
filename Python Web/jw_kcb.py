from flask import Flask, request, jsonify
import asyncio
import aiohttp
import base64
import requests
from bs4 import BeautifulSoup
import ddddocr
import json
app = Flask(__name__)
class_times = {
    1: ("08:30", "09:10"),
    2: ("09:15", "09:55"),
    3: ("10:15", "10:55"),
    4: ("11:00", "11:40"),
    5: ("11:45", "12:25"),
    6: ("13:15", "13:55"),
    7: ("14:00", "14:40"),
    8: ("14:45", "15:25"),
    9: ("15:45", "16:25"),
    10: ("16:30", "17:10"),
    11: ("17:15", "17:55"),
    12: ("19:30", "20:10"),
    13: ("20:15", "20:55"),
    14: ("21:00", "21:40"),
}
LOGIN_URL = "https://jw.gdip.edu.cn/jsxsd/xk/LoginToXk"
MAIN_URL = "http://jw.gdip.edu.cn/jsxsd/framework/main_index_loadkb.jsp"
HEADERS = {
        "Host": "jw.gdip.edu.cn",
        "Content-Length": "56",
        "Accept": "text/html, */*; q=0.01",
        "X-Requested-With": "XMLHttpRequest",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.134 Safari/537.36",
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
        "Origin": "http://jw.gdip.edu.cn",
        "Referer": "http://jw.gdip.edu.cn/jsxsd/framework/xsMain_new.jsp?t1=1",
        "Accept-Encoding": "gzip, deflate",
        "Accept-Language": "zh-CN,zh;q=0.9"
    }
def encode_credential(credential):
    return base64.b64encode(credential.encode('utf-8')).decode('utf-8')
def login(loginid, password,retry_count=5):
    bloginid = encode_credential(loginid)
    bpassword = encode_credential(password)
    response = requests.request("GET", url="https://jw.gdip.edu.cn/")
    ck = requests.utils.dict_from_cookiejar(response.cookies)
    response = requests.request("GET", url="https://jw.gdip.edu.cn/jsxsd/verifycode.servlet")
    ck2 = requests.utils.dict_from_cookiejar(response.cookies)
    ck = 'JSESSIONID=' + ck['JSESSIONID'] + '; SERVERID=' + ck2['SERVERID'] + '; JSESSIONID=' + ck2['JSESSIONID']
    ocr = ddddocr.DdddOcr()
    code = ocr.classification(response.content)
    payload = "userAccount=" + str(loginid) + "&userPassword=&RANDOMCODE=" + str(code) + "&encoded=" + str(
        bloginid) + "%25%25%25" + str(bpassword) + "&pwdstr1=&pwdstr2="
    HEADERS['Cookie'] = str(ck)
    response = requests.request("POST", LOGIN_URL, headers=HEADERS, data=payload, allow_redirects=False)
    soup = BeautifulSoup(response.text, 'html.parser')
    msg = soup.find('li', id='showMsg')
    try:
        if msg is None:
            return ck
        elif "用户名或密码错误" in str(msg):
            return jsonify({"status": "error", "data": "用户名或密码错误"})
        elif "验证码错误" in str(msg):
            if retry_count > 0:
                return login(loginid, password, retry_count-1)
            else:
                return jsonify({"status": "error", "data": "请重试"})
        else:
            return jsonify({"status": "error", "data": "未知错误"})
    except Exception as e:
        return jsonify({"status": "error", "data": e})
    
async def kb(loginid, password):
    ck = login(loginid, password)
    if isinstance(ck, str):
        HEADERS['Cookie'] = str(ck)
        payloads = "rq="+"2023-08-27"+"&sjmsValue=96BE0728A94D4297E0530100007F0427"
        async with aiohttp.ClientSession() as session:
            async with session.post(MAIN_URL, headers=HEADERS, data=payloads) as response:
                con = await response.text()
                soup = BeautifulSoup(con, 'html.parser')
                course_info_tags = soup.find_all('p', title=True)
                courses_info = [tag['title'].split('<br/>') for tag in course_info_tags]
                updated_courses_info = []
                for course_info in courses_info:
                    course_info_dict = {}
                    for info in course_info:
                        key, value = info.split('：')
                        course_info_dict[key] = value
                    original_class_time = course_info_dict['上课时间']
                    class_time_keys = original_class_time.split(' ')[-1].strip('[]节').split('-')
                    new_class_time = [class_times[int(class_time_keys[0])][0], class_times[int(class_time_keys[-1])][1]]
                    course_info_dict['上课日期'] = original_class_time
                    course_info_dict['上课时间'] =  new_class_time
                    updated_courses_info.append(course_info_dict)
                return updated_courses_info
    else:
        return ck
@app.route('/handle_kb', methods=['POST'])
def handle_kb():
    loginid = request.json.get('loginid')
    password = request.json.get('password')
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    result = loop.run_until_complete(kb(loginid=loginid, password=password))
    if isinstance(result, list):
        response = {"status": "success", "data": {"loginid": loginid, "courses": result}}
        return app.response_class(
            response=json.dumps(response, ensure_ascii=False),
            status=200,
            mimetype='application/json'
        )
    elif result.json['status'] == 'error':
        return app.response_class(
            response=json.dumps(result.json, ensure_ascii=False),
            status=200,
            mimetype='application/json'
        )
@app.route('/login', methods=['POST'])
def handle_login():
    loginid = request.json.get('loginid')
    password = request.json.get('password')
    response = login(loginid, password)
    if isinstance(response, str):
        return app.response_class(
            response=json.dumps({"status": "success", "data": response}, ensure_ascii=False),
            status=200,
            mimetype='application/json'
        )
    elif response.json['status'] == 'error':
        return app.response_class(
            response=json.dumps(response.json, ensure_ascii=False),
            status=200,
            mimetype='application/json'
        )
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)