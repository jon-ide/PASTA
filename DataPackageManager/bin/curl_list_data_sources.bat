@ECHO OFF
SET SERVICE_HOST=https://pasta.lternet.edu
SET SCOPE=edi
SET IDENTIFIER=101
SET REVISION=1

curl -i -G %SERVICE_HOST%/package/sources/eml/%SCOPE%/%IDENTIFIER%/%REVISION%
