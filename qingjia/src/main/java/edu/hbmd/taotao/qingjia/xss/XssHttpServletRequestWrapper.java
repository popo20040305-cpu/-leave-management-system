package edu.hbmd.taotao.qingjia.xss;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    private  XssProperties xssProperties;
    private Whitelist whitelist = null;
    public XssHttpServletRequestWrapper(HttpServletRequest request, XssProperties xssProperties) {
        super(request);
        this.xssProperties = xssProperties;
        switch (xssProperties.getWhiteList())
        {
            case "none":
            {
                whitelist = Whitelist.none();
                break;
            }
            case "simpleText":
            {
                whitelist = Whitelist.simpleText();
                break;
            }
            case "basic":
            {
                whitelist = Whitelist.basic();
                break;
            }
            case "basicWithImages":
            {
                whitelist = Whitelist.basicWithImages();
                break;
            }
            case "relaxed":
            {
                whitelist = Whitelist.relaxed();
                break;
            }
        }
    }
    @Override
    public String[] getParameterValues(String name) {
        String[] params = super.getParameterValues(name);
        if(params != null){
            for (int i=0; i<params.length; i++) {
                params[i] = Jsoup.clean(params[i], whitelist).trim();
            }
        }
        return params;
    }
    @Override
    public String getParameter(String name) {
        String param = super.getParameter(name);
        if (param==null){
            return param;
        }
        String result = Jsoup.clean(param, whitelist);
        return result;
    }

}
