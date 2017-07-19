package io.chengguo.apidebugger.presenter;

import io.chengguo.apidebugger.engine.http.ArtHttp;
import io.chengguo.apidebugger.engine.http.FormRequestBuilder;
import io.chengguo.apidebugger.engine.interf.ArtHttpListener;
import io.chengguo.apidebugger.engine.log.Log;
import io.chengguo.apidebugger.engine.utils.IOUtil;
import io.chengguo.apidebugger.engine.utils.StringUtils;
import io.chengguo.apidebugger.ui.iview.IHttpView;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.util.TextUtils;
import org.codehaus.jettison.json.JSONException;

import javax.swing.*;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Vector;

/**
 * Created by fingerart on 17/7/11.
 */
public class DebuggerSession implements ArtHttpListener {

    private IHttpView mView;

    public DebuggerSession(IHttpView view) {
        mView = view;
    }

    /**
     * 执行http请求
     */
    public void execute() {
        String url = mView.url().trim();
        if (validUrl(url)) {
            System.out.println("url not empty");
            return;
        }
        switch (mView.method()) {
            case "GET":
                get(url);
                break;
            case "POST":
                post(url);
                break;
        }
    }

    private void get(String url) {
        Map<String, String> headers = mView.headers();
        ArtHttp.get()
                .url(url)
                .addHeader(headers)
                .build()
                .execute(this);
    }

    private void post(String url) {
        Map<String, String> headers = mView.headers();

        FormRequestBuilder builder =
                ArtHttp.post()
                        .url(url)
                        .addHeader(headers);

        String bodyType = mView.bodyType();
        switch (bodyType) {
            case "FormData"://todo 情况复杂！！
                Map<String, String> formData = mView.bodyFormData();
                builder.addParam(formData);
                break;
            case "XWwwFormUrlencoded":
                Map<String, String> urlencode = mView.bodyUrlencode();
                builder.addParam(urlencode)
                        .xWwwUrlencoded()
                        .build()
                        .execute(this);
                break;
            case "Raw":
                String raw = mView.bodyRaw();
                builder.raw()
                        .addRaw(raw)
                        .build()
                        .execute(this);
                break;
            case "Binary":
                String filePath = mView.bodyBinary();
                File file = new File(filePath);
                builder.binary()
                        .addFile(file)
                        .build()
                        .execute(this);
                break;
            default:
                Log.e("error type");
        }
    }

    private boolean validUrl(String url) {
        return TextUtils.isEmpty(url);
    }

    @Override
    public void onPre() {
        System.out.println("DebuggerSession.onPre");
    }

    @Override
    public void onSuccess(HttpResponse response) {
        try {
            Vector<String> cookies = new Vector<>();
            Vector<String> headers = new Vector<>();
            Header[] allHeaders = response.getAllHeaders();
            for (Header header : allHeaders) {
                if ("Set-Cookie".equals(header.getName())) {
                    for (String cg : header.getValue().trim().split(";")) {
                        String[] cookie = cg.trim().split("=");
                        cookies.add(cookie[0] + " → " + cookie[1]);
                    }
                } else {
                    headers.add(header.getName() + " → " + header.getValue());
                }
            }

            mView.setCookies(cookies);
            mView.setHeaders(headers);

            String text = IOUtil.outputString(response.getEntity().getContent());
            String textFormat = formatContent(text, response.getHeaders("Content-Type"));
            Log.d(textFormat);
            SwingUtilities.invokeLater(() -> {
                mView.showPretty(textFormat);
                mView.showRaw(text);
                mView.showPreview(textFormat);
            });
        } catch (JSONException e) {
            Log.e(e);
        } catch (IOException e) {
            Log.e(e);
        } catch (TransformerException e) {
            Log.e(e);
        }
    }

    private String formatContent(String text, Header[] contentTypes) throws JSONException, TransformerException {
        Header contentType = contentTypes[0];
        if ("text/html".equalsIgnoreCase(contentType.getValue())) {
            return StringUtils.formatXml(text);
        } else if ("application/xml".equalsIgnoreCase(contentType.getValue())) {
            return StringUtils.formatXml(text);
        } else if ("application/json".equalsIgnoreCase(contentType.getValue())) {
            return StringUtils.formatJson(text);
        }
        return text;
    }

    @Override
    public void onError(Exception e) {
        System.out.println("e = [" + e + "]");
    }

    @Override
    public void onFinish() {
        System.out.println("DebuggerSession.onFinish");
    }
}










