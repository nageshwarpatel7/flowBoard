#!/bin/bash
TOKEN="squ_YOUR_TOKEN"
for SERVICE in auth-service workspace-service board-service list-service card-service notification-service; do
  echo "Analyzing $SERVICE..."
  cd $SERVICE
  mvn clean verify sonar:sonar -Dsonar.token=squ_klkgl -q
  cd ..
done
echo "All done. View results at http://localhost:9000"