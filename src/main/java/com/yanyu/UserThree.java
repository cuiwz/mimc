package com.yanyu;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xiaomi.mimc.*;
import com.xiaomi.mimc.common.MIMCConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Scanner;

/**
 * @Author cuiwz
 * @Date 2020/6/15 15:21
 * @Description TODO
 */
public class UserThree {

    private static Logger LOGGER = LoggerFactory.getLogger(UserThree.class);

    private static final String url = "https://mimc.chat.xiaomi.net/api/account/token";
    private static final long appId = 2882303761517669588L;
    private static final String appKey = "5111766983588";
    private static final String appSecurity = "b0L3IOz/9Ob809v8H2FbVg==";

    private final String appAccount = "UserThree";
    private MIMCUser user;

    public UserThree() throws Exception {
        /**
         * @param[appId]: 应用ID，小米开放平台申请分配
         * @param[appAccount]: 用户在APP帐号系统内的唯一帐号ID
         * @param[logCachePath]: 必须传入合法的路径，用于缓存日志信息
         */
        user = MIMCUser.newInstance(appId, appAccount, "./files"); // 创建一个用户
        init(user);
    }

    private void init(final MIMCUser mimcUser) throws Exception {
        mimcUser.registerTokenFetcher(new UserThree.MIMCCaseTokenFetcher(appId, appKey, appSecurity, url, mimcUser.getAppAccount()));
        mimcUser.registerOnlineStatusListener(new MIMCOnlineStatusListener() {
            public void statusChange(MIMCConstant.OnlineStatus status, String errType, String errReason, String errDescription) {
                LOGGER.info("OnlineStatusHandler, Called, {}, isOnline:{}, errType:{}, :{}, errDesc:{}",
                        mimcUser.getAppAccount(), status, errType, errReason, errDescription);
            }
        });
        mimcUser.registerMessageHandler(new MIMCMessageHandler() {
            public boolean handleMessage(List<MIMCMessage> packets) {
                for (MIMCMessage p : packets) {
                    try {
                        Msg msg = JSON.parseObject(new String(p.getPayload()), Msg.class);
                        LOGGER.info("ReceiveMessage, P2P, {}-->{}, packetId:{}, payload:{}",
                                p.getFromAccount(), p.getToAccount(), p.getPacketId(), new String(msg.getContent()));
                    } catch (Exception e) {
                        LOGGER.info("ReceiveMessage, P2P, {}-->{}, packetId:{}, payload:{}",
                                p.getFromAccount(), p.getToAccount(), p.getPacketId(), new String(p.getPayload()));
                    }
                }
                return true;
            }

            public boolean handleGroupMessage(List<MIMCGroupMessage> packets) {
                return true;
            }

            public boolean handleUnlimitedGroupMessage(List<MIMCGroupMessage> list) {
                return true;
            }

            public void handleServerAck(MIMCServerAck serverAck) {
//                LOGGER.info("ReceiveMessageAck, serverAck:{}", serverAck);
            }

            public void handleSendMessageTimeout(MIMCMessage message) {
//                LOGGER.info("handleSendMessageTimeout, packetId:{}", message.getPacketId());
            }

            public void handleSendGroupMessageTimeout(MIMCGroupMessage groupMessage) {}

            public void handleSendUnlimitedGroupMessageTimeout(MIMCGroupMessage groupMessage) {}

            public boolean onPullNotification() {
                return true;
            }

            public void handleOnlineMessage(MIMCMessage mimcMessage) {

            }

            public void handleOnlineMessageAck(MIMCOnlineMessageAck mimcOnlineMessageAck) {

            }
        });
    }

    public void ready() throws Exception {
        user.login();
    }

    public void offline() throws Exception {
        user.logout();
        user.destroy();
    }

    public void sendMessage(String account, String message) throws Exception {
        while (!user.isOnline()) {
            Thread.sleep(200);
        }

        Msg msg = new Msg();
        msg.setVersion(Constant.VERSION);
        msg.setMsgId(msg.getMsgId());
        msg.setTimestamp(System.currentTimeMillis());
        msg.setContent(message.getBytes(Charset.forName("UTF-8")));

        String jsonStr = JSON.toJSONString(msg);
        user.sendMessage(account, jsonStr.getBytes(), Constant.TEXT);

    }

    public static void main(String[] args) throws Exception {
        UserThree user = new UserThree();
        user.ready();
        Scanner sc = new Scanner(System.in);
        try {
            while (true) {
                String msg = sc.nextLine();
                if ("exit".equals(msg))
                    break;
                user.sendMessage("UserOne", msg);
                user.sendMessage("UserTwo", msg);
            }
        } finally {
            user.offline();
            sc.close();
        }
    }

    public static class MIMCCaseTokenFetcher implements MIMCTokenFetcher {
        private String httpUrl;
        private long appId;
        private String appKey;
        private String appSecret;
        private String appAccount;

        public MIMCCaseTokenFetcher(long appId, String appKey, String appSecret, String httpUrl, String appAccount) {
            this.httpUrl = httpUrl;
            this.appId = appId;
            this.appKey = appKey;
            this.appSecret = appSecret;
            this.appAccount = appAccount;
        }

        /**
         * @important: 此例中，fetchToken()直接上传(appId/appKey/appSecurity/appAccount)给小米TokenService，获取Token使用
         * 实际上，在生产环境中，fetchToken()应该只上传appAccount+password/cookies给AppProxyService，AppProxyService
         * 验证鉴权通过后，再上传(appId/appKey/appSecurity/appAccount)给小米TokenService，获取Token后返回给fetchToken()
         * @important: appKey/appSecurity绝对不能如此用例一般存放于APP本地
         **/
        public String fetchToken() throws Exception {
            URL url = new URL(httpUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.addRequestProperty("Content-Type", "application/json");

            JSONObject obj = new JSONObject();
            obj.put("appId", appId);
            obj.put("appKey", appKey);
            obj.put("appSecret", appSecret);
            obj.put("appAccount", appAccount);

            con.getOutputStream().write(obj.toString().getBytes("utf-8"));
            if (200 != con.getResponseCode()) {
                LOGGER.error("con.getResponseCode()!=200");
                System.exit(0);
            }

            String inputLine;
            StringBuffer content = new StringBuffer();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((inputLine = in.readLine()) != null) {
                content.append(trim(inputLine));
            }
            in.close();
            LOGGER.info(content.toString());

            return content.toString();
        }

        public String trim(String str) {
            return str == null ? null : str.trim();
        }
    }

}
