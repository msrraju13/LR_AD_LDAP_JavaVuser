/*
 * LoadRunner Java script. (Build: _build_number_)
 * 
 * Script Description: 
 *                     
 */

import lrapi.lr;
import java.io.*;
import java.util.*;
import java.util.Properties;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class Actions
{
	//public class LDAPTest {

	DirContext ctx;	
	Actions ldapTest;
	
	public int init() throws Throwable {
		
		//change this path as per keystore path
		//C:\Users\IUSR_TEST\Desktop
		String keystorePath = "keystore";
		System.setProperty("javax.net.ssl.keyStore", keystorePath);
		System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
		System.setProperty("javax.net.ssl.trustStore", keystorePath);
		System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

		ldapTest = new Actions();

		//Connect to the LDAP server & Authenticate with a service account
		ctx = ldapTest.establishConnection("ldaps://vds-qp.homedepot.com:1636",
				"CN=svc_vds_perftest service account,OU=Deny Logon,OU=Service Accounts,OU=Servers & Applications,ou=amer.homedepot.com,o=thdad",
				"TestMe123!");
		//System.out.prin
	return 0;
	
	}//end of init

	public int action() throws Throwable {
		
		//Search for the user you want to authenticate, search him with samaccountname attribute like TESTVDS164642 & Get the DN of the user we found
		String userDN =  ldapTest.searchUsers(ctx, "o=thdad", ""+lr.eval_string("<prmsamuser>")+"");
		
		//Open another connection to the LDAP server with the found DN and the password
		DirContext searchUserCtx = ldapTest.establishConnection("ldaps://vds-qp.homedepot.com:1636",userDN,"P@ssw0rd123");
		
		//Close search user connection
		ldapTest.closeConnection(searchUserCtx);
	return 0;
	}//end of action

	public int end() throws Throwable {
		
		//Close Service account connection
		ldapTest.closeConnection(ctx);
		return 0;
	}//end of end

//Java complete code

public DirContext establishConnection(String url, String user, String password) {
		Properties env = new Properties();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, url);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, user);
		env.put(Context.SECURITY_CREDENTIALS, password);
		try {
			lr.start_transaction("Bind User");
			ctx = new InitialDirContext(env);
			lr.end_transaction("Bind User",lr.PASS);
			System.out.println("User Authenticated! " + ctx.getEnvironment());
		} catch (AuthenticationException ex) {
			lr.end_transaction("Bind User",lr.FAIL);
			System.out.println(ex.getMessage());
		} catch (NamingException e) {
			lr.end_transaction("Bind User",lr.FAIL);
			e.printStackTrace();
		}
		return ctx;
	}	
		

	// validate share point access group
	public String searchUsers(DirContext ctx, String searchObj, String user) {

		String userDn = null;
		String searchFilter = "(samaccountname=" + user + ")";
		String[] reqAtt = { "cn", "mail", "sharepointgroup" };
		SearchControls controls = new SearchControls();
		controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		controls.setReturningAttributes(reqAtt);

		NamingEnumeration<SearchResult> users;
		SearchResult result = null;

		try {
			lr.start_transaction("Search User");
			users = ctx.search(searchObj, searchFilter, controls);
			lr.end_transaction("Search User",lr.PASS);
			if (users.hasMore()) {
				result = (SearchResult) users.next();
				userDn = result.getNameInNamespace();
				Attributes attr = result.getAttributes();
				System.out.println(attr.get("cn"));
				System.out.println(attr.get("mail"));
				System.out.println(attr.get("sharepointgroup"));
				System.out.println(userDn);
			}
		} catch (NamingException e) {
			lr.end_transaction("Search User",lr.FAIL);
			e.printStackTrace();
		}
		return userDn;

	}

	public void closeConnection(DirContext ctx) {
		try {
			lr.start_transaction("UnBind User");
			ctx.close();
			lr.end_transaction("UnBind User",lr.PASS);
		} catch (NamingException e) {
			lr.end_transaction("UnBind User",lr.FAIL);
			e.printStackTrace();
		}

	}

}

