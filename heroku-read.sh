#!/usr/bin/env bash
# Usage: ./heroku-read.sh > heroku.sh

printf "heroku config:set GITHUB_CLIENT_ID="
heroku config:get GITHUB_CLIENT_ID

printf "heroku config:set GITHUB_CLIENT_SECRET="
heroku config:get GITHUB_CLIENT_SECRET

printf "heroku config:set GITHUB_CALLBACK_URL="
heroku config:get GITHUB_CALLBACK_URL

printf "heroku config:set APPLICATION_SALT="
heroku config:get APPLICATION_SALT

printf "heroku config:set MONGO_CLIENT_URI="
heroku config:get MONGO_CLIENT_URI

printf "heroku config:set ADMIN_GITHUB_ID="
heroku config:get ADMIN_GITHUB_IDS

printf "heroku config:set GITHUB_ORG="
heroku config:get GITHUB_ORG
