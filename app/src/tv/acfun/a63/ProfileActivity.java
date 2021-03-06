/*
 * Copyright (C) 2013 YROM.NET
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tv.acfun.a63;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import tv.acfun.a63.api.ArticleApi;
import tv.acfun.a63.api.entity.User;
import tv.acfun.a63.base.BaseWebViewActivity;
import tv.acfun.a63.util.DocumentRequest;
import tv.acfun.a63.util.MemberUtils;
import android.os.Bundle;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.umeng.analytics.MobclickAgent;

/**
 * @author Yrom
 *
 */
public class ProfileActivity extends BaseWebViewActivity {

    
    protected static final String TAG = "ProfileActivity";
    private User mUser;
    private Listener<Document> mSplashListener = new Listener<Document>() {

        @Override
        public void onResponse(Document response) {
            String data = response.html();
            mWeb.loadDataWithBaseURL(getBaseUrl(), data, "text/html", "utf-8", null);
            setSupportProgressBarIndeterminateVisibility(false);
        }
        
    }; 
    private ErrorListener mErrorListner = new ErrorListener() {

        @Override
        public void onErrorResponse(VolleyError error) {
            setSupportProgressBarIndeterminateVisibility(false);
            showErrorDialog();
        }
    };
    private Cookie[] cookies;

    protected String getBaseUrl() {
        return ArticleApi.getDomainRoot(getApplicationContext());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        getSupportActionBar().setTitle(R.string.pofile);
        mWeb.addJavascriptInterface(new ACJSObject(), "AC");
    }

    @Override
    protected void initData() {
        super.initData();
        mUser = AcApp.getUser();
        if(mUser == null) {
            finish(); 
            return;
        }
        cookies = JSON.parseObject(mUser.cookies, Cookie[].class);
        SplashDocumentRequest request = new SplashDocumentRequest(cookies, mSplashListener, mErrorListner);
        request.setTag(TAG);
        AcApp.addRequest(request);
    }
    
    private class SplashDocumentRequest extends DocumentRequest{ 

        public SplashDocumentRequest(Cookie[] cookies, Listener<Document> listener,
                ErrorListener errorListner) {
            super(ArticleApi.getSplashUrl(getApplicationContext()), cookies, listener, errorListner);
        }

        @Override
        protected Document parse(String htmlFromNet) {
            try {
                InputStream in = getAssets().open("splash.html");
                Document doc = Jsoup.parse(in, "utf-8","");
                doc.getElementById("area-cont-splash").html(htmlFromNet);
                remove(doc.getElementById("btn-toggle-info"));
                remove(doc.getElementById("hint-unread-splash"));
                String btnHtml = getOptionsButtonHtml();
                Element list = doc.getElementById("info-hidden-splash");
                if(list != null)
                    list.before(btnHtml);
                else
                    doc.append(btnHtml);
                return doc;
            } catch (IOException ignored) {
            }
            
            return null;
        }

        private String getOptionsButtonHtml() {
            return "<div id=\"control\"><a class=\"button\" href=\"javascript:window.AC.logout();\" >注销</a> &nbsp;&nbsp;&nbsp; <a class=\"button\" href=\"javascript:window.AC.checkin();\">签到</a> </div>";
        }
        void remove(Element e){
            if(e != null) e.remove();
        }
        
    }
    class ACJSObject {
        @android.webkit.JavascriptInterface
        public void logout(){
            AcApp.logout();
            setResult(RESULT_OK);
            MobclickAgent.onEvent(ProfileActivity.this, "log_out");
            finish();
        }
        @android.webkit.JavascriptInterface
        public void checkin(){
            JSONObject checkIn = MemberUtils.checkIn(getBaseUrl(), cookies);
            if(checkIn != null){
                if(checkIn.getBooleanValue("success")){
                    AcApp.showToast(getString(R.string.check_in_success));
                }else{
                    AcApp.showToast(getString(R.string.check_in_failed)+checkIn.getString("result"));
                }
            }else{
                AcApp.showToast(getString(R.string.check_in_failed)+getString(R.string.retry_pls));
            }
            
                            
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        AcApp.cancelAllRequest(TAG);
    }
}
