#!/bin/bash

# Start tail.
<#list tailPatterns as tailPattern>
export GREP_COLOR="01;32"
touch "${tailPattern.filePath}"
tail -F -n0 "${tailPattern.filePath}" | grep --line-buffered --color=always -A10 -B10 "${tailPattern.pattern}" 2>&1 > ${executionStagingDir}/tailoutput${executionId}${tailPattern?index} &
pid${tailPattern?index}=$!
</#list>

WINGS_SCRIPT_NAME=$1
shift

# Run the script
$WINGS_SCRIPT_DIR/$WINGS_SCRIPT_NAME

bold=$(tput bold)
normal=$(tput sgr0)
boldgreen='\x1b[1;32m'
boldred='\x1b[1;31m'


echo " "
echo " "
echo -e "${r"${bold}"}Starting tail log verification${r"${normal}"}"

#Wait for tail outputs.
TAIL_TIMEOUT=30
TAIL_COUNT=${tailPatterns?size}
while [ "$TAIL_TIMEOUT" -gt 0 -a "$TAIL_COUNT" -gt 0 ]
do
<#list tailPatterns as tailPattern>
  if [ $(wc -l ${executionStagingDir}/tailoutput${executionId}${tailPattern?index} | tr -s " " | cut -d " " -f2) -gt 0 ]
  then
    if $(kill -0 $pid${tailPattern?index} 2>/dev/null)
    then
      kill -9 $pid${tailPattern?index} || true
      TAIL_COUNT=$((TAIL_COUNT - 1))
    fi
  fi
</#list>
  sleep 1;
  TAIL_TIMEOUT=$((TAIL_TIMEOUT - 1))
done

#Kill remaining tails after timeout.
<#list tailPatterns as tailPattern>
if $(kill -0 $pid${tailPattern?index} 2>/dev/null)
then
  kill -9 $pid${tailPattern?index} || true
fi
</#list>

returnvalue=0

#Print outputs.
<#list tailPatterns as tailPattern>
echo " "
echo " "
echo "===================================================================================================="
printf "Searching file ${r"${bold}"}'${tailPattern.filePath}'${r"${normal}"} for pattern ${r"${boldgreen}"}'${tailPattern.pattern}'${r"${normal}"} ... "
if [ $(wc -l ${executionStagingDir}/tailoutput${executionId}${tailPattern?index} | tr -s " " | cut -d " " -f2) -gt 0 ]
then
  printf "${r"${boldgreen}"}[Found]${r"${normal}"}\n"
  echo "===================================================================================================="
  echo "Output: "
  cat ${executionStagingDir}/tailoutput${executionId}${tailPattern?index}
else
  printf "${r"${boldred}"}[Not Found]${r"${normal}"}\n"
  returnvalue=1
fi
</#list>

if [ "$returnvalue" -eq 1 ]
then
  #Summarize failures
  echo " "
  echo " "
  printf "${r"${bold}"}Unable to following patterns: ${r"${normal}"}\n"
  <#list tailPatterns as tailPattern>
  if [ $(wc -l ${executionStagingDir}/tailoutput${executionId}${tailPattern?index} | tr -s " " | cut -d " " -f2) -eq 0 ]
  then
    echo "File: '${tailPattern.pattern}'"
    echo "Pattern: '${tailPattern.filePath}'"
  fi
</#list>
fi

echo " "
echo " "
echo "${r"${bold}"}Tail log verification finished${r"${normal}"}"


exit $returnvalue
