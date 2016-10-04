#!/usr/bin/env bash
# Usage: ./heroku-read.sh > heroku.sh

printf "heroku config:set GITHUB_CLIENT_ID="
heroku config:get GITHUB_CLIENT_ID --app ucsb-cs56-f16

printf "heroku config:set GITHUB_CLIENT_SECRET="
heroku config:get GITHUB_CLIENT_SECRET --app ucsb-cs56-f16

printf "heroku config:set GITHUB_CALLBACK_URL="
heroku config:get GITHUB_CALLBACK_URL --app ucsb-cs56-f16

printf "heroku config:set APPLICATION_SALT="
heroku config:get APPLICATION_SALT --app ucsb-cs56-f16

printf "heroku config:set MONGO_CLIENT_URI="
heroku config:get MONGO_CLIENT_URI --app ucsb-cs56-f16

printf "heroku config:set ADMIN_GITHUB_ID="
heroku config:get ADMIN_GITHUB_IDS --app ucsb-cs56-f16

printf "heroku config:set GITHUB_ORG="
heroku config:get GITHUB_ORG --app ucsb-cs56-f16
