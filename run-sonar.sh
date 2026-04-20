#!/bin/bash
TOKEN="squ_YOUR_TOKEN"
for SERVICE in auth-service workspace-service board-service list-service card-service notification-service; do
  echo "Analyzing $SERVICE..."
  cd $SERVICE
  mvn clean verify sonar:sonar -Dsonar.token=squ_af87a29525b846d789a1c65223da7d960eb8fb41 -q
  cd ..
done
echo "All done. View results at http://localhost:9000"