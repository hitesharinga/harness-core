#!/bin/bash
PROJECTS="BT|CCE|CCM|CDC|CDNG|CDP|CE|CI|CV|CVNG|DEL|DOC|DX|ER|FFM|OPS|PL|SEC|SWAT|GTM|ONP"
KEYS=$(git log --pretty=oneline --abbrev-commit |\
      awk "/${PREVIOUS_CUT_COMMIT_MESSAGE}/ {exit} {print}" |\
      grep -o -iE '('$PROJECTS')-[0-9]+' | sort | uniq)

# QA-Test status ID is 201, Dev Complete status id is 151.
STATUS_ID=201

if [ ! -z "$STATUS_ID_TO_MOVE" ]
then
      STATUS_ID=$STATUS_ID_TO_MOVE
fi

for KEY in ${KEYS}
do
    echo $KEY

    status=`curl -X GET -H "Content-Type: application/json" https://harness.atlassian.net/rest/api/2/issue/${KEY}?fields=status --user $JIRA_USERNAME:$JIRA_PASSWORD | jq ".fields.status.name" | tr -d '"'`

    echo $status

    if [[ "${status}" = "QA Test" || "${status}" = "Done" ]]
            then
               echo " ${KEY}  is in Done or QA-Test status, Hence no update"
            else
               echo " ${KEY}  is in  ${status} , Hence moving to ${STATUS_ID} status"
               curl \
                 -X POST \
                --data "{\"transition\":{\"id\":\"${STATUS_ID}\"}}" \
                -H "Content-Type: application/json" \
                https://harness.atlassian.net/rest/api/2/issue/${KEY}/transitions \
                --user $JIRA_USERNAME:$JIRA_PASSWORD
    fi

done

