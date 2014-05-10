package com.nsn.lib.pushserver;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;






import javax.naming.AuthenticationException;
import javax.naming.NamingEnumeration;

import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;


@SuppressWarnings("rawtypes")

public class LdapUserAuthenticate {
    private String URL = "ldap://10.135.55.17:389/";
    private String BASEDN = "o=nsn";
    private String FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    private LdapContext ctx = null;
	private Hashtable env = null;
    private Control[] connCtls = null;
   
   
    @SuppressWarnings("unchecked")
	private void LDAP_connect(){
        env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY,FACTORY);
        env.put(Context.PROVIDER_URL, URL+BASEDN);//LDAP server
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        //此处若不指定用户名和密码,则自动转换为匿名登录
        try{
            ctx = new InitialLdapContext(env,connCtls);
        }catch(javax.naming.AuthenticationException e){
            System.out.println("Authentication faild: "+e.toString());
        }catch(Exception e){
            System.out.println("Something wrong while authenticating: "+e.toString());
        }
    }
   
   
    private String getUserDN(String uid){
    	 String userDN = "";   
         
         LDAP_connect();  
          
         try{  
                SearchControls constraints = new SearchControls();  
                constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);  
                NamingEnumeration en = ctx.search("","uid="+uid, constraints); //The UID you are going to query,* means all nodes  
                if(en == null){  
                 System.out.println("Have no NamingEnumeration.");  
                }  
                if(!en.hasMoreElements()){  
                 System.out.println("Have no element.");  
                }  
                while (en != null && en.hasMoreElements()){//maybe more than one element  
                    Object obj = en.nextElement();  
                    if(obj instanceof SearchResult){  
                        SearchResult si = (SearchResult) obj;  
                        userDN += si.getName();  
                        userDN += "," + BASEDN;  
                    }  
                    else{  
                        System.out.println(obj);  
                    }  
                    System.out.println();  
                }  
               }catch(Exception e){  
                System.out.println("Exception in search():"+e);  
               }  
          
         return userDN;  
    }
   
   
    public boolean authenricate(String ID,String password){
    	if(password==null||"".equals(password.trim())){
    		 System.out.println("password could not be empty");  
    		return false;
    	}
    	boolean valide = false;  
        String userDN = getUserDN(ID);  
        try {  
            ctx.addToEnvironment(Context.SECURITY_PRINCIPAL,userDN);  
            ctx.addToEnvironment(Context.SECURITY_CREDENTIALS,password);  
            ctx.reconnect(connCtls);  
            System.out.println(userDN + " is authenticated(验证成功！)");  
            valide = true;  
        }catch (AuthenticationException e) {  
            System.out.println(userDN + " is not authenticated");  
            System.out.println(e.toString());  
            valide = false;  
        }catch (NamingException e) {  
            System.out.println(userDN + " is not authenticated");  
            valide = false;  
        }  
         
        return valide;  
    }
    public static void main(String[] args){
    	LdapUserAuthenticate u=new LdapUserAuthenticate();
    	
    	System.out.println(u.authenricate("yuywu","3"));
    }
}