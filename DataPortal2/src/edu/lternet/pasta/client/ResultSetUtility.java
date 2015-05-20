/*
 *
 * $Date$
 * $Author$
 * $Revision$
 *
 * Copyright 2011,2012 the University of New Mexico.
 *
 * This work was supported by National Science Foundation Cooperative
 * Agreements #DEB-0832652 and #DEB-0936498.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 *
 */

package edu.lternet.pasta.client;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.xpath.CachedXPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import edu.lternet.pasta.portal.search.PageControl;
import edu.lternet.pasta.portal.search.Search;
import edu.lternet.pasta.portal.user.SavedData;


/**
 * @author servilla
 * @since Apr 6, 2012
 * 
 *        The ResultSetUtility class parses XML search results and
 *        renders them as HTML.
 * 
 */
public class ResultSetUtility {

  /*
   * Class variables
   */

  private static final Logger logger = Logger
      .getLogger(edu.lternet.pasta.client.ResultSetUtility.class);
  
  /*
   * Instance variables
   */

  private String resultSet = null;
  private int rows = Search.DEFAULT_ROWS;
  private Integer numFound = 0;
  private Integer start = 0;
  private PageControl pageControl = null;
  private String pageBodyHTML = "";
  private String pageHeaderHTML = "";
  private SavedData savedData = null;
  private boolean isSavedDataPage;
  private boolean showSavedData = true;
  private String sort = null;
  

  /*
   * Constructors
   */

  /**
   * Constructs a new ResultSetUtility object from search results XML,
   * specifying whether the search results represent saved data packages
   * or regular search results.
   * 
   * @param xml
   *          The search results XML as a String object.
   * 
   * @throws ParseException
   */
  public ResultSetUtility(String xml, String sort, SavedData savedData, boolean isSavedDataPage) throws ParseException {

    if (xml == null || xml.isEmpty()) {
      throw new ParseException("Result Set is empty", 0);
    }

    this.resultSet = xml;
    this.sort = sort;
    this.savedData = savedData;
    this.isSavedDataPage = isSavedDataPage;
    parseResultSet(xml);
    pageControl = new PageControl(numFound, start, rows, sort, isSavedDataPage);
    pageHeaderHTML = pageControl.composePageHeader();
    pageBodyHTML = pageControl.composePageBody();
  }

  
  /**
   * Constructs a new ResultSetUtility object from search results XML.
   * 
   * @param xml
   *          The search results XML as a String object.
   * 
   * @throws ParseException
   */
  public ResultSetUtility(String xml, String sort) throws ParseException {
	  this(xml, sort, null, false);
	  this.showSavedData = false;
  }

  
  /*
   * Methods
   */
  
  
  	public Integer getNumFound() {
  		return numFound;
  	}
  
  
  	private void parseResultSet(String xml) { 	        	  
  		if (xml != null) {
  			InputStream inputStream = null;
  			try {
  				inputStream = IOUtils.toInputStream(xml, "UTF-8");
  				DocumentBuilder documentBuilder = 
  	              DocumentBuilderFactory.newInstance().newDocumentBuilder();
  				CachedXPathAPI xpathapi = new CachedXPathAPI();

  				Document document = null;
  				document = documentBuilder.parse(inputStream);
  	      
  				if (document != null) {
  	        
  					Node numFoundNode = null;
  					numFoundNode = xpathapi.selectSingleNode(document, "//resultset/@numFound");

  					if (numFoundNode != null) {
  						String numFoundStr = numFoundNode.getNodeValue();
  						this.numFound = new Integer(numFoundStr);
  					}
  					
  					Node startNode = null;
  					startNode = xpathapi.selectSingleNode(document, "//resultset/@start");

  					if (startNode != null) {
  						String startStr = startNode.getNodeValue();
  						this.start = new Integer(startStr);
  					}
  					
  				}
  			}
  			catch (Exception e) {
  		        logger.error("Error parsing search result set: " + e.getMessage());
  			}
  			finally {
  				if (inputStream != null) {
  					try {
  						inputStream.close();
  					}
  					catch (IOException e) {
  						;
  					}
  				}
  			}
  		}
  	}
  	        

  	/**
  	 * Sets the value of the docsPerPage instance variable.
  	 * 
  	 * @param n
  	 * 			the desired number of documents displayed per page
  	 */
	public void setRowsPerPage(int n) {
		this.rows = new Integer(n);
	}


	/**
	 * Transforms Solr search results XML to an HTML table.
	 * 
	 * @param xslPath
	 *            The path to the search results-set stylesheet.
	 * 
	 * @return The HTML table as a String object.
	 */
	public String xmlToHtmlTable(String xslPath) throws ParseException {
		String html = "";
		HashMap<String, String> parameterMap = new HashMap<String, String>();
		
		if (rows > 0) {
			String tableHeaderHTML = composeTableHeaderHTML(this.showSavedData);
		
			String savedDataList = "";	
			if (savedData != null) {
				savedDataList = savedData.getSavedDataList();
			}
		
			// Pass the docsPerPage value as a parameter to the XSLT
			parameterMap.put("start", start.toString());
			parameterMap.put("rows", new Integer(this.rows).toString());
			parameterMap.put("isSavedDataPage", new Boolean(this.isSavedDataPage).toString());
			parameterMap.put("savedDataList", savedDataList);
			parameterMap.put("showSavedData", new Boolean(this.showSavedData).toString());

			String tableRowsHTML = XSLTUtility.xmlToHtml(this.resultSet, xslPath,
				parameterMap);
		
			String tableFooterHTML = composeTableFooterHTML();

			html = String.format("%s%s%s%s%s<br/>%s", 
				pageHeaderHTML, pageBodyHTML, 
				tableHeaderHTML, tableRowsHTML, tableFooterHTML, 
				pageBodyHTML);
		}
		else {
			html = composeNoMatchesHTML(isSavedDataPage);
		}

		return html;
	}
	
	
	private String composeTableHeaderHTML(boolean showSavedData) {
		StringBuilder html = new StringBuilder("\n");
		html.append("<table width=\"100%\">\n");
		html.append("    <tbody>\n");
		html.append("        <tr>\n");
		
		String titleSort = pageControl.getTitleSort();
		String creatorsSort = pageControl.getCreatorsSort();
		String pubDateSort = pageControl.getPubDateSort();
		String packageIdSort = pageControl.getPackageIdSort();
		
		String servlet = isSavedDataPage ? "savedDataServlet" : "simpleSearch";
		
		if (showSavedData) {
			html.append("            <th class=\"nis\" width=\"45%\"><a class='searchsubcat' href='" + servlet + "?start=0&rows=10&sort=" + titleSort + "'>Title</a></th>\n");
    		html.append("            <th class=\"nis\" width=\"20%\"><a class='searchsubcat' href='" + servlet + "?start=0&rows=10&sort=" + creatorsSort + "'>Creators</a></th>\n");
    		html.append("            <th class=\"nis\" width=\"10%\"><a class='searchsubcat' href='" + servlet + "?start=0&rows=10&sort=" + pubDateSort + "'>Publication Date</a></th>\n");
    		html.append("            <th class=\"nis\" width=\"15%\"><a class='searchsubcat' href='" + servlet + "?start=0&rows=10&sort=" + packageIdSort + "'>Package Id</a></th>\n");
    		String dataShelfStartTag = isSavedDataPage ? "" : "<a href='savedDataServlet'>";
    		String dataShelfEndTag =   isSavedDataPage ? "" : "</a>";
    		html.append("            <th class=\"nis\" width=\"10%\">" + dataShelfStartTag + "<img alt='Your Data Shelf' src='images/data_shelf.png' title='Your Data Shelf' width='60' height='60'></img>" + dataShelfEndTag + "</th>\n");
		}
		else {
			html.append("            <th class=\"nis\" width=\"50%\"><a class='searchsubcat' href='" + servlet + "?start=0&rows=10&sort=" + titleSort + "'>Title</a></th>\n");
    		html.append("            <th class=\"nis\" width=\"25%\"><a class='searchsubcat' href='" + servlet + "?start=0&rows=10&sort=" + creatorsSort + "'>Creators</a></th>\n");
    		html.append("            <th class=\"nis\" width=\"10%\"><a class='searchsubcat' href='" + servlet + "?start=0&rows=10&sort=" + pubDateSort + "'>Publication Date</a></th>\n");
    		html.append("            <th class=\"nis\" width=\"15%\"><a class='searchsubcat' href='" + servlet + "?start=0&rows=10&sort=" + packageIdSort + "'>Package Id</a></th>\n");
		}
		
		html.append("        </tr>\n");

		String htmlStr = html.toString();
		return htmlStr;
	}

	
	private String composeTableFooterHTML() {
		StringBuilder html = new StringBuilder("\n");
		html.append("    </tbody>\n");
		html.append("</table>\n");	
		
		return html.toString();
	}

	
	private String composeNoMatchesHTML(boolean isSavedDataPage) {
		String html = "";
		
		if (isSavedDataPage) {
			html = "<p>There are no data packages on your data shelf.</p>";
		}
		else {
			html = "<p>No matching data packages were found.</p>";
		}
		
		return html;
	}

}
