<%@ page language="java" import="java.util.*" pageEncoding="ISO-8859-1"
	trimDirectiveWhitespaces="true"%>
<%
  String path = request.getContextPath();
  String basePath = request.getScheme() + "://" + request.getServerName()
      + ":" + request.getServerPort() + path + "/";

  String metadataHtml = (String) request.getAttribute("metadataHtml");
  String packageId = (String) request.getAttribute("packageId");

  if (packageId == null || packageId.isEmpty()) {
    packageId = "unknown";
  }

%>

<!DOCTYPE html>
<html>

<head>
<title>LTER :: Network Data Portal</title>

<meta content="width=device-width, initial-scale=1, maximum-scale=1" name="viewport">

<!-- Google Fonts CSS -->
<link href="https://fonts.googleapis.com/css?family=Open+Sans:400,300,600,300italic" rel="stylesheet" type="text/css">

<!-- Page Layout CSS MUST LOAD BEFORE bootstap.css -->
<link href="css/style_slate.css" media="all" rel="stylesheet" type="text/css">

<!-- JS -->
<script src="js/jqueryba3a.js?ver=1.7.2" type="text/javascript"></script>
<script src="bootstrap/js/bootstrap68b368b3.js?ver=1" type="text/javascript"></script>
<script src="js/jquery.easing.1.368b368b3.js?ver=1" type="text/javascript"></script>
<script src="js/jquery.flexslider-min68b368b3.js?ver=1" type="text/javascript"></script>
<script src="js/themeple68b368b3.js?ver=1" type="text/javascript"></script>
<script src="js/jquery.pixel68b368b3.js?ver=1" type="text/javascript"></script>
<script src="js/jquery.mobilemenu68b368b3.js?ver=1" type="text/javascript"></script>
<script src="js/isotope68b368b3.js?ver=1" type="text/javascript"></script>
<script src="js/mediaelement-and-player.min68b368b3.js?ver=1" type="text/javascript"></script>

<!-- Mobile Device CSS -->
<link href="bootstrap/css/bootstrap.css" media="screen" rel="stylesheet" type="text/css">
<link href="bootstrap/css/bootstrap-responsive.css" media="screen" rel="stylesheet" type="text/css">

<!-- Metadata Viewer -->
<link href="css/sbclter.css" rel="stylesheet" type="text/css">
<script src="js/toggle.js" type="text/javascript"></script>
<!-- /Metadata Viewer -->

</head>

<body>

<jsp:include page="header.jsp" />

<div class="row-fluid ">
		<div class="container">
			<div class="row-fluid distance_1">
				<div class="box_shadow box_layout">
					<div class="row-fluid">
						<div class="row-fluid">
							<div class="span12">
							
								<!-- Content -->
                  <%= metadataHtml %>																
							  <!-- /Content -->
							  
						  </div>
					</div>
				</div>
			</div>
		</div>
	</div>

		<jsp:include page="footer.jsp" />
		
</div>

        <script type="text/javascript">
        jQuery(document).ready(function() {
          jQuery(".toggleButton").click(function() {
            jQuery(this).next(".collapsible").slideToggle("fast");
          });
          jQuery(".collapsible").hide();
          jQuery("#toggleSummary").next(".collapsible").show();
        });    
        jQuery("#showAll").click(function() {
          jQuery(".collapsible").show();
        });
        jQuery("#hideAll").click(function() {
          jQuery(".collapsible").hide();
        });
        </script>  

</body>

</html>