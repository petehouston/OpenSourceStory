@echo off
@title Dump
set CLASSPATH=.;dist\*
java -Dwz=wz\ tools.wztosql.DumpQuests
pause