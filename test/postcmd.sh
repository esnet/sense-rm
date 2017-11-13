#! /bin/sh -x

curl -H "Content-Type: application/json" -X POST -d @delta_post.json "http://localhost:8080/api/sense/v1/deltas?encode=true"
