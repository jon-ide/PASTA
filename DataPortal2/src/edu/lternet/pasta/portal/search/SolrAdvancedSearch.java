/*
 *
 * $Date: 2012-06-22 12:23:25 -0700 (Fri, 22 June 2012) $
 * $Author: dcosta $
 * $Revision: 2145 $
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

package edu.lternet.pasta.portal.search;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import edu.lternet.pasta.client.DataPackageManagerClient;
import edu.lternet.pasta.client.PastaAuthenticationException;
import edu.lternet.pasta.client.PastaConfigurationException;
import edu.lternet.pasta.portal.search.LTERSite;


/**
 * AdvancedSearch class constructs queries that use the pathquery feature of 
 * Metacat. It can execute either an advanced search, where the user fills in
 * fields in a web form, or a simple search on a string.
 */
public class SolrAdvancedSearch extends Search  {
  
  /*
   * Class fields
   */

	private static final Logger logger = Logger.getLogger(SolrAdvancedSearch.class);

  
  /*
   * Instance fields
   */
  
  /*
   * Form parameters
   */
  private String caseSensitive;
  private final String creatorOrganization;
  private final String creatorSurname;
  private final String dateField;
  private final String startDate;
  private final String endDate;
  private final String globalOperator = "UNION";
  private int INDENT_LEVEL = 2;
  private boolean isDatesContainedChecked;
  private String namedTimescale;
  private String namedTimescaleQueryType;
  private String[] siteValues = null;
  private String subjectField;
  private String subjectValue;
  private String taxon;
  
  private boolean isBoundaryContainedChecked;
  private String boundsChangedCount;
  private String northBound;
  private String southBound;
  private String eastBound;
  private String westBound;
  private String locationName;
  
  private String queryString = "";
  private TermsList termsList;
  private final String title = "Advanced Search";

  // Controlled vocabulary settings
  private boolean hasExact = false;
  private boolean hasNarrow = false;
  private boolean hasRelated = false;
  private boolean hasNarrowRelated = false;
  private boolean hasAll = false;

  
  /*
   * Constructors
   */

  /**
   * Constructor. Passes in a large set of form field parameters.
   */
  public SolrAdvancedSearch(
      String creatorOrganization,
      String creatorSurname,
      String dateField,
      String startDate,
      String endDate,
      String namedTimescale,
      String[] siteValues,
      String subjectField,
      String subjectValue,
      boolean isDatesContainedChecked,
      boolean isSpecificChecked,
      boolean isRelatedChecked,
      boolean isRelatedSpecificChecked,
      String taxon,
      boolean isBoundaryContainedChecked,
      String boundsChangedCount,
      String northBound,
      String southBound,
      String eastBound,
      String westBound,
      String locationName
                       ) { 
    this.creatorOrganization = creatorOrganization;
    this.creatorSurname = creatorSurname;
    this.dateField = dateField;
    this.startDate = startDate;
    this.endDate = endDate;
    this.isDatesContainedChecked = isDatesContainedChecked;
    this.namedTimescale = namedTimescale;
    this.siteValues = siteValues;
    this.subjectField = subjectField;
    this.subjectValue = subjectValue;
    this.taxon = taxon;
    
    this.isBoundaryContainedChecked = isBoundaryContainedChecked;
    this.boundsChangedCount = boundsChangedCount;
    this.northBound = northBound;
    this.southBound = southBound;
    this.eastBound = eastBound;
    this.westBound = westBound;
    this.locationName = locationName;
    
    this.hasExact = !isSpecificChecked && !isRelatedChecked && !isRelatedSpecificChecked;
    this.hasNarrow = isSpecificChecked && !isRelatedChecked && !isRelatedSpecificChecked;
    this.hasRelated = !isSpecificChecked && isRelatedChecked && !isRelatedSpecificChecked;
    this.hasNarrowRelated = isSpecificChecked && isRelatedChecked && !isRelatedSpecificChecked;
    this.hasAll = isRelatedSpecificChecked;
    
    this.termsList = new TermsList();
  }

  
  /*
   * Class methods
   */

  
  /*
   * Instance methods
   */

  /**
   * A full subject query searches the title, abstract, and keyword sections of
   * the document. Individual searches on these sections is also supported.
   */
	private void buildQuerySubject(TermsList termsList) {
		List<String> terms;
		String field = "subject";
		String queryTerms = "";

		if ((this.subjectValue != null) && (!(this.subjectValue.equals("")))) {
			if (subjectField.equals("ABSTRACT")) {
				field = "abstract";
			}
			else if (subjectField.equals("KEYWORDS")) {
				field = "keyword";
			}
			else if (subjectField.equals("TITLE")) {
				field = "title";
			}

			terms = parseTerms(this.subjectValue);

			for (String term : terms) {
				TreeSet<String> derivedTerms = new TreeSet<String>();
				TreeSet<String> webTerms = ControlledVocabularyClient
						.webServiceSearchValues(term, hasExact, hasNarrow,
								hasRelated, hasNarrowRelated, hasAll);
				webTerms = optimizeTermList(webTerms);

				for (String webValue : webTerms) {
					derivedTerms.add(webValue);
					queryTerms += " " + webValue;
				}

				/*
				 * Sometimes the original search term (e.g. "fishes") doesn't
				 * need to be included in the set of search values because it is
				 * covered by a substring term returned by the vocabulary web
				 * service (e.g. "fish"). However, if the web service failed to
				 * return any values, then we need to add back the original
				 * search term.
				 */
				if (webTerms.size() < 1) {
					derivedTerms.add(term);
				}

				for (String derivedTerm : derivedTerms) {
					termsList.addTerm(derivedTerm);
				}
			}

			queryTerms = queryTerms.trim();
			String subjectQuery = String.format("%s:%s", field, queryTerms);
			this.queryString = subjectQuery;
		}
	}


  /**
   * An author query will search the creator/individualName/surName field, the
   * creator/organizationName field, or an intersection of both fields.
   */
  private void buildQueryAuthor(TermsList termsList) {
    String value = this.creatorSurname;

    if ((value != null) && (!(value.equals("")))) {
      termsList.addTerm(value);
      String authorQuery = String.format("author:%s", value);
      this.queryString = String.format("%s %s", this.queryString.trim(), authorQuery);
    }

    value = this.creatorOrganization;
      
    if ((value != null) && (!(value.equals("")))) {
      termsList.addTerm(value);
      String organizationQuery = String.format("organization:%s", value);
      this.queryString = String.format("%s %s", this.queryString.trim(), organizationQuery);
    }
    
  }
  

  /**
   * Builds query group for a search on a specific named location.
   */
  private void buildQueryGeographicDescription(String locationName, TermsList termsList) {
    if ((locationName != null) && (!(locationName.equals("")))) {
      termsList.addTerm(locationName);
      String locationQuery = String.format("geographicdescription:%s", locationName);
      this.queryString = String.format("%s %s", this.queryString.trim(), locationQuery);
    }
  }
  
  
  /**
   * Builds query group for spatial search on north/south/east/west bounding
   * coordinates. Includes logic to handle queries across the international
   * date line.
   */
  private void buildQuerySpatial(String northValue, String southValue, 
                                 String eastValue, String westValue,
                                 boolean boundaryContained) {
    boolean crosses;
    AdvancedSearchQueryGroup datelineGroup = null;
    String emlField;
    AdvancedSearchQueryGroup leftOfDateline = null;
    final String operator = "INTERSECT";
    String indent = getIndent(INDENT_LEVEL * 2);
    AdvancedSearchQueryGroup qgSpatial;
    AdvancedSearchQueryTerm qt;
    AdvancedSearchQueryGroup rightOfDateline = null;
    String searchMode;

    qgSpatial = new AdvancedSearchQueryGroup(operator, indent);
    indent = getIndent(INDENT_LEVEL * 3);
    
    northValue = validateGeographicCoordinate(northValue, -90.0F, 90.0F, "90.0");
    southValue = validateGeographicCoordinate(southValue, -90.0F, 90.0F, "-90.0");
    eastValue = validateGeographicCoordinate(eastValue, -180.0F, 180.0F, "180.0");
    westValue = validateGeographicCoordinate(westValue, -180.0F, 180.0F, "-180.0");

    /* Check that all four coordinates has values before attempting spatial
     * search.
     */
    if ((northValue != null) && 
        (!(northValue.equals(""))) &&
        (southValue != null) && 
        (!(southValue.equals(""))) &&
        (eastValue != null) && 
        (!(eastValue.equals(""))) &&
        (westValue != null) && 
        (!(westValue.equals("")))
       ) {
      
      if (Integer.valueOf(boundsChangedCount) == 1) {
        return;
      }
      
      /*
       * Check whether the east/west coordinates cross over the international
       * dateline. If the search crosses the dateline, special handling will
       * be needed in forming the query.
       */
      crosses = crossesInternationalDateline(eastValue, westValue);
      
      if (crosses) {
        datelineGroup = new AdvancedSearchQueryGroup("UNION", indent);
        indent = getIndent(INDENT_LEVEL * 4);
        leftOfDateline = new AdvancedSearchQueryGroup("INTERSECT", indent);
        rightOfDateline = new AdvancedSearchQueryGroup("INTERSECT", indent);
        datelineGroup.addQueryGroup(leftOfDateline);
        datelineGroup.addQueryGroup(rightOfDateline);
      }

      /*
       * If the user selects the boundaryContained checkbox, use the following
       * logical expression. N, S, E, and W are the boundaries of the bounding
       * box, while N', S', E', and W' are the boundaries specified in a given
       * EML document:
       *              (N' <= N) && (S' >= S) && (E' <= E) && (W' >= W)
       */
      if (boundaryContained) {

        emlField = "dataset/coverage/geographicCoverage/boundingCoordinates/northBoundingCoordinate";
        searchMode = "less-than-equals";
        qt=new AdvancedSearchQueryTerm(searchMode,caseSensitive,emlField, 
                                       northValue, indent);
        qgSpatial.addQueryTerm(qt);        

        emlField = "dataset/coverage/geographicCoverage/boundingCoordinates/southBoundingCoordinate";
        searchMode = "greater-than-equals";
        qt=new AdvancedSearchQueryTerm(searchMode,caseSensitive,emlField, 
                                       southValue, indent);
        qgSpatial.addQueryTerm(qt);        

        emlField = "dataset/coverage/geographicCoverage/boundingCoordinates/eastBoundingCoordinate";
        searchMode = "less-than-equals";
        
        if (crosses) {
          indent = getIndent(INDENT_LEVEL * 5);
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           "180.0", indent);
          leftOfDateline.addQueryTerm(qt);
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           eastValue, indent);
          rightOfDateline.addQueryTerm(qt);
        }
        else {
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           eastValue, indent);
          qgSpatial.addQueryTerm(qt);
        }

        emlField = "dataset/coverage/geographicCoverage/boundingCoordinates/westBoundingCoordinate";
        searchMode = "greater-than-equals";
        
        if (crosses) {
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           westValue, indent);
          leftOfDateline.addQueryTerm(qt);
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           "-180.0", indent);
          rightOfDateline.addQueryTerm(qt);
        }
        else {
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           westValue, indent);
          qgSpatial.addQueryTerm(qt);        
        }
      }
     /*
      * Else, if the user does not select the boundaryContained checkbox, use 
      * the following logical expression. N, S, E, and W are the boundaries of 
      * the bounding box, while N', S', E', and W' are the boundaries specified 
      * in a given EML document:
      *              (N' > S) && (S' < N) && (E' > W) && (W' < E)
      */
      else {     

        emlField = "dataset/coverage/geographicCoverage/boundingCoordinates/southBoundingCoordinate";
        searchMode = "less-than";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         northValue, indent);
        qgSpatial.addQueryTerm(qt);        

        emlField = "dataset/coverage/geographicCoverage/boundingCoordinates/northBoundingCoordinate";
        searchMode = "greater-than";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         southValue, indent);
        qgSpatial.addQueryTerm(qt);        

        emlField = "dataset/coverage/geographicCoverage/boundingCoordinates/westBoundingCoordinate";
        searchMode = "less-than";
        
        if (crosses) {
          indent = getIndent(INDENT_LEVEL * 5);
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           "180.0", indent);
          leftOfDateline.addQueryTerm(qt);
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           eastValue, indent);
          rightOfDateline.addQueryTerm(qt);
        }
        else {
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           eastValue, indent);
          qgSpatial.addQueryTerm(qt);   
        }

        emlField = "dataset/coverage/geographicCoverage/boundingCoordinates/eastBoundingCoordinate";
        searchMode = "greater-than";
        
        if (crosses) {
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           westValue, indent);
          leftOfDateline.addQueryTerm(qt);
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           "-180.0", indent);
          rightOfDateline.addQueryTerm(qt);
        }
        else {
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                           westValue, indent);
          qgSpatial.addQueryTerm(qt);
        }
      }

      if (crosses) {
        qgSpatial.addQueryGroup(datelineGroup);
      }

    }
  }
  

  /*
   * Helper method to implement server-side validation of geographic coordinate values.
   */
  private String validateGeographicCoordinate(String stringValue, 
                                              float minValue, 
                                              float maxValue, 
                                              String defaultValue) {
    String returnValue = defaultValue;
    String warning = "Illegal geographic coordinate specified: '" + stringValue +
                     "'. Expected a value between " + minValue + " and " + maxValue;

    if (stringValue == null || stringValue.equals("")) {
      return returnValue;
    }
    else {
      try {
        Float floatValue = new Float(stringValue);

        if ((floatValue != null) &&
            (floatValue >= minValue) && 
            (floatValue <= maxValue)) {
          returnValue = stringValue;
        }
        else {
          logger.warn(warning);
        }
      }
      catch (NumberFormatException e) {
        logger.warn(warning);
      }
    }
    
    return returnValue;
  }
  

  /**
   * Two kinds of temporal searches are supported. The first is on a named
   * time scale. The second is on a specific start date and/or end date.
   */
  private void buildQueryTemporalCriteria(String dateField,
                                          String startDate,
                                          String endDate,
                                          boolean isDatesContainedChecked,
                                          String namedTimescale, 
                                          String namedTimescaleQueryType,
                                          TermsList termsList
                                         ) {
    boolean addQueryGroup = false;
    boolean addQueryGroupDates = false;
    boolean addQueryGroupNamed = false;
    String emlField;
    final String operator = "INTERSECT";
    String indent = getIndent(INDENT_LEVEL * 2);
    AdvancedSearchQueryGroup qg= new AdvancedSearchQueryGroup(operator, indent);
    AdvancedSearchQueryGroup qgNamed, qgDates, qgDatesStart, qgDatesEnd;
    AdvancedSearchQueryTerm qt;
    String searchMode;
    String xDate;     // Will hold either "beginDate" or "endDate"

    indent = getIndent(INDENT_LEVEL * 3);

    /* If the user specified a named timescale, check to see whether it occurs
     * in any of three possible places: singleDateTime, beginDate, or endDate.
     */
    qgNamed = new AdvancedSearchQueryGroup("UNION", indent);
    if ((namedTimescale != null) && (!(namedTimescale.equals("")))) {
      indent = getIndent(INDENT_LEVEL * 4);
      searchMode = metacatSearchMode(namedTimescaleQueryType);
      
      emlField = 
           "temporalCoverage/singleDateTime/alternativeTimeScale/timeScaleName";
      qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                       namedTimescale, indent);
      qgNamed.addQueryTerm(qt);
      
      emlField = 
   "temporalCoverage/rangeOfDates/beginDate/alternativeTimeScale/timeScaleName";
      qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                       namedTimescale, indent);
      qgNamed.addQueryTerm(qt);
      
      emlField = 
     "temporalCoverage/rangeOfDates/endDate/alternativeTimeScale/timeScaleName";
      qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                       namedTimescale, indent);
      qgNamed.addQueryTerm(qt);
      termsList.addTerm(namedTimescale);
      
      addQueryGroupNamed = true;
    }
    
    qgDates = new AdvancedSearchQueryGroup("INTERSECT", indent);
    
    startDate = validateDateString(startDate);
    endDate = validateDateString(endDate);
    validateDateRange(startDate, endDate);

    // If a start date was specified, search for temporal coverage and/or a
    // pubDate with 'endDate' greater than or equal to the specified start date.
    //
    if ((startDate != null) && (!(startDate.equals("")))) {
      indent = getIndent(INDENT_LEVEL * 4);
      qgDatesStart = new AdvancedSearchQueryGroup("UNION", indent);
      indent = getIndent(INDENT_LEVEL * 5);
      searchMode = "greater-than-equals";

      if (dateField.equals("ALL") || dateField.equals("COLLECTION")) {
        xDate = isDatesContainedChecked ? "beginDate" : "endDate";
        emlField = "temporalCoverage/rangeOfDates/" + xDate + "/calendarDate";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         startDate, indent);
        qgDatesStart.addQueryTerm(qt);        

        emlField = "temporalCoverage/singleDateTime/calendarDate";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         startDate, indent);
        qgDatesStart.addQueryTerm(qt);
      }
      
      if (dateField.equals("ALL") || dateField.equals("PUBLICATION")) {
        emlField = "dataset/pubDate";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         startDate, indent);
        qgDatesStart.addQueryTerm(qt);        
      }
      
      qgDates.addQueryGroup(qgDatesStart);
      addQueryGroupDates = true;
    }

    // If an end date was specified, search for temporal coverage and/or a
    // pubDate with 'beginDate' less than or equal to the end date.
    //
    if ((endDate != null) && (!(endDate.equals("")))) {
      indent = getIndent(INDENT_LEVEL * 4);
      qgDatesEnd = new AdvancedSearchQueryGroup("UNION", indent);
      indent = getIndent(INDENT_LEVEL * 5);
      searchMode = "less-than-equals";

      if (dateField.equals("ALL") || dateField.equals("COLLECTION")) {
        xDate = isDatesContainedChecked ? "endDate" : "beginDate";
        emlField = "temporalCoverage/rangeOfDates/" + xDate + "/calendarDate";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         endDate, indent);
        qgDatesEnd.addQueryTerm(qt);        

        emlField = "temporalCoverage/singleDateTime/calendarDate";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         endDate, indent);
        qgDatesEnd.addQueryTerm(qt);
      }
      
      if (dateField.equals("ALL") || dateField.equals("PUBLICATION")) {
        emlField = "dataset/pubDate";
        qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                         endDate, indent);
        qgDatesEnd.addQueryTerm(qt);        
      }      

      qgDates.addQueryGroup(qgDatesEnd);
      addQueryGroupDates = true;
    }
    
    if (addQueryGroupNamed) {
      qg.addQueryGroup(qgNamed);
      addQueryGroup = true;
    }
    
    if (addQueryGroupDates) {
      qg.addQueryGroup(qgDates);
      addQueryGroup = true;
    }
    
  }
  
  
  /*
   * Check whether a user's input date string conforms with one of the
   * allowed formats as specified on the web form.
   */
  private String validateDateString(String dateString) {
    String returnValue = null;
    DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM");
    DateFormat dateFormat3 = new SimpleDateFormat("yyyy");
    Date date = null;
    
    if (dateString == null || dateString.equals("")) {
      return dateString;
    }
    else {
      try {
        date = dateFormat1.parse(dateString);
        returnValue = dateFormat1.format(date);
      }
      catch (ParseException e1) {
        try {
          date = dateFormat2.parse(dateString);
          returnValue = dateFormat2.format(date);
        }
        catch (ParseException e2) {
          try {
            date = dateFormat3.parse(dateString);
            returnValue = dateFormat3.format(date);
          }
          catch (ParseException e3) {
            logger.warn("Couldn't parse date string using any of the recognized formats: " + dateString);
          }    
        }
      }
    }
    
    return returnValue;
  }
  
  
  /*
   * Check whether a user's input date range is valid.
   */
  private void validateDateRange(String startDateStr, String endDateStr) {
    Date startDate = null;
    Date endDate = null;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    
    try {
      if ((startDateStr != null) && (endDateStr != null)) {
        startDate = dateFormat.parse(startDateStr);
        endDate = dateFormat.parse(endDateStr);
      
        if ((startDate != null) && (endDate != null) && (startDate.after(endDate))) {
          throw new IllegalArgumentException(
            "The date range is invalid. Start date ('" + startDateStr + 
            "') should be less than end date ('" + endDateStr + "').");   	
        }
      }
    }
    catch (ParseException e) {
      logger.warn("Couldn't parse date string: " + e.getMessage());
    }
    
  }


  /**
   * A taxon query searches the taxonRankValue field,
   * matching the field if the user-specified value is contained in the field.
   */
  private void buildQueryTaxon(TermsList termsList) {
    final String value = this.taxon;
      
    if ((value != null) && (!(value.equals("")))) {
      termsList.addTerm(value);
      String taxonQuery = String.format("taxonomic:%s", value);
      this.queryString = String.format("%s %s", this.queryString.trim(), taxonQuery);
    }
  }
  

  /**
   * Build a site filter. If the AdvancedSearch's site value is non-null, add a
   * query group that limits the results to a particular LTER site. Do this
   * by searching for a packageId attribute that starts with "knb-lter-xyz"
   * where "xyz" is the three-letter site acronym, or for a site keyword
   * phrase (e.g. "Kellogg Biological Station") anywhere in the documment.
   */
  private void buildSiteFilter(TermsList termsList) {
    String attributeValue = "";
    String emlField = "";
    String indent = getIndent(INDENT_LEVEL * 2);
    final String operator = "UNION";
    AdvancedSearchQueryGroup qg= new AdvancedSearchQueryGroup(operator, indent);
    AdvancedSearchQueryTerm qt;
    String searchMode;
   
    indent = getIndent(INDENT_LEVEL * 3);

    if (this.siteValues != null) {
      for (int i = 0; i < siteValues.length; i++) {  
        String site = siteValues[i];
        LTERSite lterSite = new LTERSite(site);
        if (lterSite.isValidSite()) {
          emlField = "@packageId";
          attributeValue = lterSite.getPackageId();              
          searchMode = "starts-with";
          qt = new AdvancedSearchQueryTerm(searchMode, caseSensitive, emlField, 
                                       attributeValue, indent);
          qg.addQueryTerm(qt);
          String siteName = lterSite.getSiteName();
          if ((siteName != null) && (!siteName.equals(""))) {
            termsList.addTerm(siteName);
          }
        }
      }

    }
  }


  /**
   * Boolean to determine whether the east/west coordinates of a spatial search
   * cross over the international date line.
   * 
   * @param eastValue
   * @param westValue
   * @return true if the values cross the data line, else false.
   */
  private boolean crossesInternationalDateline(String eastValue, 
                                               String westValue
                                               ) {
    boolean crosses = false;
    
    if ((eastValue != null) &&
        (eastValue != "") &&
        (westValue != null) &&
        (westValue != "")
       ) {
      Double eastInteger = new Double(eastValue);
      Double westInteger = new Double(westValue);
      
      crosses = westInteger > eastInteger;
    }
    
    logger.debug("crosses International Dateline: " + crosses);
    
    return crosses;
  }

  
	/**
	 * Builds and runs a search, returning the result XML string.
	 * 
	 * @param request
	 *            the servlet request object
	 * @param uid
	 *            the user id
	 */
	public String executeSearch(final HttpServletRequest request,
			final String uid) {
		String resultsetXML = null;
		String htmlMessage = null;
		buildQuerySubject(this.termsList);
		buildQueryAuthor(this.termsList); 
		buildQueryTaxon(this.termsList);
		buildQueryGeographicDescription(this.locationName, this.termsList);
		
		queryString = queryString.trim();

		/*
		 * buildQuerySpatial(this.northBound, this.southBound, this.eastBound,
		 * this.westBound, this.isBoundaryContainedChecked);
		 * buildQueryTemporalCriteria(this.dateField, this.startDate,
		 * this.endDate, this.isDatesContainedChecked, this.namedTimescale,
		 * this.namedTimescaleQueryType, this.termsList);
		 * buildSiteFilter(termsList);
		 */

		try {
			DataPackageManagerClient dpmClient = new DataPackageManagerClient(
					uid);
			logger.warn(String.format("queryString:\n%s", queryString));
			resultsetXML = dpmClient.searchDataPackages(queryString);
		}
		catch (PastaAuthenticationException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			htmlMessage = "<p class=\"warning\">" + e.getMessage() + "</p>\n";
		}
		catch (PastaConfigurationException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			htmlMessage = "<p class=\"warning\">" + e.getMessage() + "</p>\n";
		}
		catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			htmlMessage = "<p class=\"warning\">" + e.getMessage() + "</p>\n";
		}

		request.setAttribute("searchresult", htmlMessage);

		return resultsetXML;
	}


  /**
   * Returns a string of spaces that corresponds to the current indent level.
   * 
   * @param indentLevel   The number of spaces to be indented.
   * @return              A string containing indentLevel number of spaces.
   */
  private String getIndent(final int indentLevel) {
    StringBuffer indent = new StringBuffer(12);
    
    for (int i = 0; i < indentLevel; i++) {
      indent.append(" ");
    }
    
    return indent.toString();
  }
  
  
  /*
   * Accessor method for the termsList instance variable.
   * 
   * @return  termsList, a TermsList object
   */
  public TermsList getTermsList() {
    return termsList;
  }
  
  
  /**
   * Given a query type value, return the corresponding Metacat
   * searchmode string.
   * 
   * @param queryType  A string value indicating the query type:
   *                     "0" --> "contains"
   *                     "1" --> "equals"
   *                     "2" --> "starts-with"
   *                     "3" --> "ends-with"
   * @return A string, the Metacat search mode value.
   */ 
  private String metacatSearchMode(final String queryType) {   
    if (queryType == null) return "contains";
    if (queryType.equals("0")) return "contains";
    if (queryType.equals("1")) return "equals";
    if (queryType.equals("2")) return "starts-with";
    if (queryType.equals("3")) return "ends-with";
    return "contains";  
  }
  
  
  /*
   * Optimize the set of search terms returned from the controlled
   * vocabulary web service by removing terms that are already
   * covered by substring terms. For example, "fishes" can be
   * removed if "fish" is already present.
   * 
   * An exception to this rule is if the substring term is short
   * enough for using "exact" instead of "contains" (i.e. less than
   * or equal to AdvancedSearchQueryGroup.EXACT_SEARCH_MAXIMUM_LENGTH).
   */
  private TreeSet<String> optimizeTermList(TreeSet<String> webValues) {
    TreeSet<String> optimizedWebValues = new TreeSet<String>();
    
    for (String value1 : webValues) {
      if (value1 != null) {
        boolean keepThisValue = true;
        for (String value2 : webValues) {
          if ((!value1.equalsIgnoreCase(value2)) &&
              (value2.length() > 
               AdvancedSearchQueryGroup.EXACT_SEARCH_MAXIMUM_LENGTH)
             ) {
            if (value1.contains(value2)) {
              keepThisValue = false; // covered by a substring term
            }
          }
        }
        
        /*
         * This is a workaround for a Metacat bug #5443
         * 
         * http://bugzilla.ecoinformatics.org/show_bug.cgi?id=5443
         * 
         * Metacat structured pathquery is bugged. It does not support
         * searchmode of 'equals' or 'matches-exactly' the way it should.
         * So we need to filter out any terms that are two characters
         * or less in length because they match too many documents on
         * a 'contains' search.
         */
        if (value1.length() <= 3) {
          keepThisValue = false;
        }
      
        if (keepThisValue) {
          optimizedWebValues.add(value1);
        }
      }
    }
    
    return optimizedWebValues;
  }
  
}
