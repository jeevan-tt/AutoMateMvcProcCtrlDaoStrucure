@echo off
set /p name=Enter TABLE name: 
echo %name% > temp.txt
java SpringMvcCodeGenerator < temp.txt
del temp.txt
echo Code generation completed successfully!