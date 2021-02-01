  /*
 * Example Active Directory LoadRunner script (written in Java)
 * 
 * Script Description: 
 *     This script binds to active directory using LDAP protocol, then search for the user in active directory 
 *     and finally unbinds.
 *
 * You will probably need to add the keystore files to Extrafiles if testing over SSL
 *   
 */

import lrapi.lr;l
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

	DirContext ctx;	
	Actions ldapTest;
	
	public int init() throws Throwable {
		
		//Line 36 to 40 are needed only if we are tetsing against a SSL enabled active directory server, if there is no SSL, comment them
		//change this path as per keystore path, if you added keysore file to Extra Files in LR they file name alone should be enough
		String keystorePath = "keystore";
		System.setProperty("javax.net.ssl.keyStore", keystorePath);
		System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
		System.setProperty("javax.net.ssl.trustStore", keystorePath);
		System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

		ldapTest = new Actions();

		//Connect to the LDAP server & Authenticate with a service account
		ctx = ldapTest.establishConnection("ldaps://ad-test.org.com:636",
				"uid=testuser,ou=people,dc=org,dc=com",
				"TestPassword");
		//System.out.prin
	return 0;
	
	}//end of init

	public int action() throws Throwable {
		
		//Search for the user you want to authenticate, search him with samaccountname attribute like TESTVDS164642 & Get the DN of the user we found
		String userDN =  ldapTest.searchUsers(ctx, "o=org", "<prmsamuser>");
		
		//Open another connection to the LDAP server with the found DN and the password
		DirContext searchUserCtx = ldapTest.establishConnection("ldaps://ad-test.org.com:636",userDN,"TestPassword");
		
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
			lr.output_message("User Authenticated! " + ctx.getEnvironment());
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

