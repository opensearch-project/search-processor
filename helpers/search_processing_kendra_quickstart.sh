#!/bin/bash

set -o errexit
set -o errtrace
set -o pipefail
set -o nounset

# Some useful constants
readonly DOCKER_IMAGE_TAG="opensearch-with-ranking-plugin"
readonly OPENSEARCH_VERSION="2.4.0"

# Uncomment the following for prerelease testing:
readonly SEARCH_PROCESSOR_PLUGIN_URL="https://github.com/msfroh/search-relevance/releases/download/v2.4.0-alpha/search-processor-disable-ssl-validation.zip"

#
# Set default values for OpenSearch (+Dashboards) image tags and plugin URL.
# We can pass them from outside to override these settings for testing purposes.
#
if [ -z "${OPENSEARCH_IMAGE_TAG:-}" ]; then
  OPENSEARCH_IMAGE_TAG="opensearchproject/opensearch:${OPENSEARCH_VERSION}"
fi
if [ -z "${OPENSEARCH_DASHBOARDS_IMAGE_TAG:-}" ]; then
  OPENSEARCH_DASHBOARDS_IMAGE_TAG="opensearchproject/opensearch-dashboards:${OPENSEARCH_VERSION}"
fi
if [ -z "${SEARCH_PROCESSOR_PLUGIN_URL:-}" ]; then
  SEARCH_PROCESSOR_PLUGIN_URL="https://github.com/opensearch-project/search-relevance/releases/download/${OPENSEARCH_VERSION}/search-processor.zip"
fi

function print_help() {
  cat << EOF
Usage: $0 [-p <execution_plan_id>] [-r <region>] [-e <kendra_ranking_endpoint>]
        [--profile <AWS profile name>] [--create-execution-plan]
  -p | --execution-plan-id          The ID returned from Kendra Intelligent Ranking service
                                    from the call to CreateRescoreExecutionPlan. Required if
                                    --create-execution-plan is not set.
  -r | --region                     The AWS region for the Kendra Intelligent Ranking
                                    service endpoint. If not specified, will read from the
                                    AWS CLI for the default profile.
  -e | --kendra-ranking-endpoint    The URL for the Kendra Intelligent Ranking service
                                    endpoint. If not specified, a value based on the
                                    region will be used.
  --profile                         The AWS profile to use for credentials. If not set, then
                                    the script will try first to use credentials from the
                                    environment, then from the default AWS profile.
  --create-execution-plan           If set, then the supplied AWS credentials will be used to
                                    create a Kendra Intelligent Ranking execution plan with
                                    1 capacity unit. NOTE: You will be charged for provisioned
                                    execution plan.

  NOTE: If the --profile option is not specified, the script will attempt to read AWS 
  credentials (access/secret key, optional session token) from environment variables, 
  and then from the default AWS profile, in order to pass them to the OpenSearch keystore
  to be used to connect to the Kendra Intelligent ranking service.
  If no credentials are found, the script WILL NOT pass credentials to the OpenSearch
  keystore. When running a reranking request, the ranking plugin may rely on
  instance profile credentials delivered through the EC2 metadata service, or credentials
  from the ECS metadata service.
EOF
}

#
# Parse and validate arguments
#

while [ "$#" -gt 0 ]; do
    case $1 in
        -p | --execution-plan-id )
            shift
            EXECUTION_PLAN_ID=$1
            shift
            ;;
        -r | --region )
            shift
            AWS_REGION=$1
            shift
            ;;
        -e | --kendra-ranking-endpoint )
            shift
            KENDRA_RANKING_ENDPOINT=$1
            shift
	    ;;
        -h | --help )
	    print_help
            exit 0
            ;;
	--profile )
	    shift
	    AWS_PROFILE=$1
	    shift
	    ;;
        --create-execution-plan )
            shift
            CREATE_EXECUTION_PLAN=1
            ;;
    esac
done

FAILED_VALIDATION=0
if [ -z "${EXECUTION_PLAN_ID:-}" ] && [ -z "${CREATE_EXECUTION_PLAN:-}" ]; then
  >&2 echo "Missing argument [-p | --execution-plan] or --create-execution-plan"
  FAILED_VALIDATION=1
elif [ -n "${EXECUTION_PLAN_ID:-}" ] && [ -n "${CREATE_EXECUTION_PLAN:-}" ]; then
  >&2 echo "You cannot specify both [-p | --execution-plan] and --create-execution-plan"
  FAILED_VALIDATION=1
fi

# End of required arguments. If any are missing, print help and exit.
if [ "${FAILED_VALIDATION}" == "1" ]; then
  echo
  print_help
  exit 1
fi

#
# Determine which credentials and region to use. By the end of this block, all specified 
# credentials will be loaded into environment variables (or we fail with an explanatory 
# error message).
#
if [ -n "${AWS_PROFILE:-}" ]; then
  # Load everything from the specified profile
  AWS_ACCESS_KEY_ID=$(aws --profile ${AWS_PROFILE} configure get aws_access_key_id || echo)
  AWS_SECRET_ACCESS_KEY=$(aws --profile ${AWS_PROFILE} configure get aws_secret_access_key || echo)
  AWS_SESSION_TOKEN=$(aws --profile ${AWS_PROFILE} configure get aws_session_token || echo)
  if [ -z "${AWS_ACCESS_KEY_ID:-}" ] || [ -z "${AWS_SECRET_ACCESS_KEY:-}" ]; then
    >&2 echo "Unable to load credentials from profile ${AWS_PROFILE}"
    exit 1
  elif [ -z "${AWS_SESSION_TOKEN}" ]; then
    echo "Using AWS credentials (aws_access_key_id and aws_secret_access_key) from profile ${AWS_PROFILE}"
  else 
    echo "Using AWS credentials (aws_access_key_id, aws_secret_access_key, and aws_session_token) from profile ${AWS_PROFILE}"
  fi
  if [ -z "${AWS_REGION:-}" ]; then
    AWS_REGION=$(aws --profile ${AWS_PROFILE} configure get region || echo)
    if [ -n "${AWS_REGION:-}" ]; then
      echo "Using AWS region ${AWS_REGION} from profile ${AWS_PROFILE}"
    else
      >&2 echo "Argument [-r | --region] not specified and unable to infer region from profile ${AWS_PROFILE}"
      exit 1
    fi
  fi
else
  # No profile set
  if [ -z "${AWS_ACCESS_KEY_ID:-}" ]; then
    if [ -z "${AWS_SECRET_ACCESS_KEY:-}" ]; then
      echo "No profile set and no credentials in environment. Trying to load default profile credentials."
      AWS_ACCESS_KEY_ID=$(aws configure get aws_access_key_id || echo)
      AWS_SECRET_ACCESS_KEY=$(aws configure get aws_secret_access_key || echo)
      AWS_SESSION_TOKEN=$(aws configure get aws_session_token || echo)
      if [ -z "${AWS_ACCESS_KEY_ID:-}" ] || [-z "${AWS_SECRET_ACCESS_KEY}" ]; then
        echo "Unable to load credentials from default profile. No credentials will be passed to the OpenSearch keystore."
        echo "OpenSearch will use the default credential provider chain to access Kendra, which may rely on EC2 instance"
        echo "profile credentials or credentials from ECS metadata service."
      elif [ -z "${AWS_SESSION_TOKEN}" ]; then
        echo "Using AWS credentials (aws_access_key_id and aws_secret_access_key) from default profile."
      else 
        echo "Using AWS credentials (aws_access_key_id, aws_secret_access_key, and aws_session_token) from default profile."
      fi
    else
      >&2 echo "Environment variable AWS_SECRET_ACCESS_KEY is specified, but AWS_ACCESS_KEY_ID is not."
      >&2 echo "Unable to determine which credentials to use."
      exit 1
    fi
  else
    # AWS_ACCCESS_KEY_ID is set
    if [ -z "${AWS_SECRET_ACCESS_KEY:-}" ]; then
      >&2 echo "Environment variable AWS_ACCESS_KEY_ID is specified, but AWS_SECRET_ACCESS_KEY is not."
      >&2 echo "Unable to determine which credentials to use."
      exit 1
    else
      if [ -n "${AWS_SESSION_TOKEN:-}"]; then
        echo "Using credentials from environment (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, and AWS_SESSION_TOKEN)."
      else
        echo "Using credentials from environment (AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY)."
      fi
    fi
  fi
  if [ -z "${AWS_REGION:-}" ]; then
    AWS_REGION=$(aws configure get region || echo)
    if [ -n "${AWS_REGION:-}" ]; then
      echo "Using AWS region ${AWS_REGION} from default profile"
    else
      >&2 echo "Argument [-r | --region] not specified and unable to infer region from default profile"
      exit 1
    fi
  fi
fi

#
# Infer Kendra endpoint from region if missing.
#
if [ -z "${KENDRA_RANKING_ENDPOINT:-}" ]; then
  KENDRA_RANKING_ENDPOINT="https://kendra-ranking.${AWS_REGION}.api.aws"
  echo "Missing argument [-e | --kendra-ranking-endpoint] -- using ${KENDRA_RANKING_ENDPOINT}"
fi

#
# Check if a rescore execution plan with name TestPlan already exists. 
# If so, try to reuse it, since it was (probably) created by an earlier run of this script.
# Otherwise, create a new rescore execution plan with default capacity.
#
if [ -n "${CREATE_EXECUTION_PLAN:-}" ]; then
  if ! aws kendra-ranking help > /dev/null 2>1; then 
    >&2 echo "AWS CLI does not support kendra-ranking service. Please install the latest AWS CLI."
    exit 1
  fi
  if [ -n "${AWS_ACCESS_KEY_ID:-}" ] && [ -n "${AWS_SECRET_ACCESS_KEY:-}" ]; then
    export AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY
    if [ -n "${AWS_SESSION_TOKEN:-}" ]; then
      export AWS_SESSION_TOKEN
    fi
  fi
  PLAN_NAME="TestPlan"
  EXISTING_EXECUTION_PLANS=$(aws ${AWS_CLIENT_OPTS:-} --region ${AWS_REGION} --endpoint-url ${KENDRA_RANKING_ENDPOINT} \
	  kendra-ranking list-rescore-execution-plans --query SummaryItems --output text | sed 's/\t/,/g' )
  for p in ${EXISTING_EXECUTION_PLANS[@]}; do
    CURRENT_PLAN_ID=$(echo $p | cut -d, -f2)
    CURRENT_PLAN_NAME=$(echo $p | cut -d, -f3)
    CURRENT_PLAN_STATUS=$(echo $p | cut -d, -f4)
    if [ "${CURRENT_PLAN_NAME}" == "${PLAN_NAME}" ]; then
      if [ "${CURRENT_PLAN_STATUS}" == "ACTIVE" ] || [ "${CURRENT_PLAN_STATUS}" == "CREATING" ]; then
        EXECUTION_PLAN_ID=${CURRENT_PLAN_ID}
        STATUS=${CURRENT_PLAN_STATUS}
        echo "Reusing existing execution plan named ${PLAN_NAME} with ID ${EXECUTION_PLAN_ID} and status ${STATUS}"
	break
      elif [ "${CURRENT_PLAN_STATUS}" == "DELETING" ]; then
        echo "Ignoring execution plan named ${PLAN_NAME} with ID ${CURRENT_PLAN_ID} and status ${CURRENT_PLAN_STATUS}."
      else
        >&2 echo "Found an execution plan named ${PLAN_NAME} with ID ${CURRENT_PLAN_ID} and status ${CURRENT_PLAN_STATUS}."
        >&2 echo "Please delete it before proceeding."
        exit 1
      fi
    fi
  done

  if [ -z "${EXECUTION_PLAN_ID:-}" ]; then 
    EXECUTION_PLAN_ID=$(aws ${AWS_CLIENT_OPTS:-} --region ${AWS_REGION} --endpoint-url ${KENDRA_RANKING_ENDPOINT} \
      kendra-ranking create-rescore-execution-plan --name "TestPlan" \
        --description "Plan created by search_processing_kendra_quickstart.sh" \
        --output text --query Id)
    echo "Created execution plan with ID ${EXECUTION_PLAN_ID}. "
    STATUS=$(aws ${AWS_CLIENT_OPTS:-} --region ${AWS_REGION} --endpoint-url ${KENDRA_RANKING_ENDPOINT} \
       kendra-ranking describe-rescore-execution-plan --id ${EXECUTION_PLAN_ID} --output text --query Status)
  fi
  if ! [ "${STATUS}" == "ACTIVE" ]; then
    echo "Waiting for it to become active. This may take a couple of minutes."
    while ! [ "${STATUS:-}" == "ACTIVE" ]; do
      sleep 10
      echo "Checking status every 10 seconds..."
      STATUS=$(aws ${AWS_CLIENT_OPTS:-} --region ${AWS_REGION} --endpoint-url ${KENDRA_RANKING_ENDPOINT} \
         kendra-ranking describe-rescore-execution-plan --id ${EXECUTION_PLAN_ID} --output text --query Status)
    done
    echo "Execution plan ${EXECUTION_PLAN_ID} is now active."
  fi
fi
    
#
# Create a unique directory to hold the Dockerfile and docker-compose.yml files.
#
PLATFORM=$(uname)
if [ "${PLATFORM}" == "Darwin" ]; then
  DOCKER_BUILD_DIR=$(mktemp -d opensearch-kendra-ranking-docker.XXXX)
else
  # Assume GNU mktemp
  DOCKER_BUILD_DIR=$(mktemp -d -p . opensearch-kendra-ranking-docker.XXXX)
fi
cd ${DOCKER_BUILD_DIR}

SUFFIX=$(echo ${DOCKER_BUILD_DIR} | sed s/.*\.//)
echo $SUFFIX

echo "Running in $(pwd)"

#
# Construct a Dockerfile that installs the search-processor plugin in the target image
#
cat >Dockerfile <<EOF
# Use base OpenSearch image
FROM ${OPENSEARCH_IMAGE_TAG}

# Install the plugin
RUN /usr/share/opensearch/bin/opensearch-plugin install --batch ${SEARCH_PROCESSOR_PLUGIN_URL}
EOF

#
# If credentials were resolved above, push them to the OpenSearch keystore in the Docker image.
#
if [ -n "${AWS_ACCESS_KEY_ID:-}" ] && [ -n "${AWS_SECRET_ACCESS_KEY:-}" ]; then
  touch kendra_ranking.credentials
  chmod 600 kendra_ranking.credentials
  cat >>kendra_ranking.credentials <<EOF
kendra_intelligent_ranking.aws.access_key:$AWS_ACCESS_KEY_ID
kendra_intelligent_ranking.aws.secret_key:$AWS_SECRET_ACCESS_KEY
EOF
  if [ -n "${AWS_SESSION_TOKEN:-}" ]; then
    # If a session token was found in the environment / profile, push it to the OpenSearch keystore too.
    echo "WARNING: Adding session token to OpenSearch keystore. These temporary credentials will"
    echo "expire, and should only be used for short-lived testing."
    cat >>kendra_ranking.credentials <<EOF
kendra_intelligent_ranking.aws.session_token:$AWS_SESSION_TOKEN
EOF
  fi
  cat >install_credentials.sh <<"EOF"
#!/bin/bash

for l in $(cat $1); do
  KEY=$(echo $l | cut -f1 -d:)
  VALUE=$(echo $l | cut -f2 -d:)
  echo $VALUE | /usr/share/opensearch/bin/opensearch-keystore add $KEY --stdin
done
EOF
  chmod 755 install_credentials.sh
  cat >>Dockerfile << EOF

# Push credentials to keystore
COPY --chown=opensearch:opensearch install_credentials.sh /tmp
COPY --chown=opensearch:opensearch kendra_ranking.credentials /tmp
RUN /usr/share/opensearch/bin/opensearch-keystore create
RUN --mount=type=secret,id=credentials,target=/tmp/kendra_ranking.credentials,required=true,mode=0444 \
  /tmp/install_credentials.sh /tmp/kendra_ranking.credentials
EOF
fi

# 
# Build and tag the Docker image with the plugin (and maybe credentials in the keystore)
#
if [ -f kendra_ranking.credentials ]; then
  DOCKER_BUILDKIT=1 docker build --tag ${DOCKER_IMAGE_TAG} --secret id=credentials,src=kendra_ranking.credentials .
  rm kendra_ranking.credentials
else
  docker build --tag ${DOCKER_IMAGE_TAG} .
fi

#
# Make sure we have opensearch-dashboards:
#
docker pull ${OPENSEARCH_DASHBOARDS_IMAGE_TAG}

# 
# Create a docker-compose.yml file that will launch an OpenSearch node with the image we
# just built and an OpenSearch Dashboards node that points to the OpenSearch node.
#
cat >docker-compose.yml <<EOF
version: '3'
networks:
  opensearch-net:
services:
  opensearch-node:
    image: ${DOCKER_IMAGE_TAG}
    container_name: opensearch-node
    environment: 
      - cluster.name=opensearch-cluster
      - node.name=opensearch-node
      - discovery.type=single-node
      - kendra_intelligent_ranking.service.endpoint=${KENDRA_RANKING_ENDPOINT}
      - kendra_intelligent_ranking.service.region=${AWS_REGION}
      - kendra_intelligent_ranking.service.execution_plan_id=${EXECUTION_PLAN_ID}
    ulimits:
      memlock:
        soft: -1
        hard: -1
      nofile:
        soft: 65536
        hard: 65536
    ports:
      - 9200:9200
      - 9600:9600
    networks:
      - opensearch-net
  opensearch-dashboard:
    image: ${OPENSEARCH_DASHBOARDS_IMAGE_TAG}
    container_name: opensearch-dashboards
    ports:
      - 5601:5601
    environment:
      OPENSEARCH_HOSTS: '["https://opensearch-node:9200"]'
    networks:
      - opensearch-net
EOF

# 
# Bring up the images
#
docker-compose up -d

#
# Generate a cleanup script. This can be run later to clean up resources created by this run.
#
cat >cleanup_resources.sh <<EOF
#!/bin/bash
echo "This script will shut down and clean up running Docker containers for OpenSearch and OpenSearch Dashboards."
if [ -n "${CREATE_EXECUTION_PLAN:-}" ]; then
  echo "And it will delete the Kendra Intelligent Ranking rescore execution plan with ID ${EXECUTION_PLAN_ID}"
fi

RESPONSE="unknown"
while ! [ "\${RESPONSE}" == "yes" ] && ! [ "\${RESPONSE}" == "no" ]; do
  echo "Are you sure? (yes/no)"
  read RESPONSE
done
if [ "\${RESPONSE}" == "yes" ]; then
  cd \$(dirname \$0)
  docker-compose down
  if [ -n "${CREATE_EXECUTION_PLAN:-}" ]; then
    echo "Deleting rescore execution plan with ID ${EXECUTION_PLAN_ID}..."
    if [ -n "${AWS_PROFILE:-}" ]; then
      PROFILE_OPT="--profile ${AWS_PROFILE:-}"
    fi
    aws ${AWS_CLIENT_OPTS:-} \${PROFILE_OPT:-} --region ${AWS_REGION} --endpoint-url ${KENDRA_RANKING_ENDPOINT} \
         kendra-ranking delete-rescore-execution-plan --id ${EXECUTION_PLAN_ID}
  fi
fi  
EOF
chmod +x cleanup_resources.sh

#
# Output helpful "getting started" text.
#
cat >README <<EOF

OpenSearch container launched, listening on port 9200.
OpenSearch Dashboards container launched, listening on port 5601.

Interact with OpenSearch using curl by authenticating as admin:admin like:
  curl -ku "admin:admin" https://localhost:9200/

Index some data on OpenSearch by following instructions at 
https://opensearch.org/docs/latest/opensearch/index-data/


Connect to OpenSearch Dashboards with a web browser at http://localhost:5601/,
using username admin and password admin. Select "Search Relevance" from the
top-left menu. In the resulting UI, you can submit a query without Kendra
Intelligent ranking and one with Kendra Intelligent Ranking.

To add Kendra Intelligent Ranking to your query add an "ext" section as follows:

{
  "query" :{
     // Your existing query
  },
  "ext": {
    "search_configuration":{
      "result_transformer" : {
        "kendra_intelligent_ranking": {
          "order": 1,
          "properties": {
            "title_field": "<title_field>",
            "body_field": "<body_field>"
          }
        }
      }
    }
  }
}

where <body_field> is the name of a field containing the main text to be 
analyzed for rescoring and <title_field> is the name of an (optional) field 
representing the title of your documents.

Interact with the Docker containers using docker-compose from directory
  $(pwd)

Some helpful docker-compose commands:

  docker-compose logs opensearch-node 
                Outputs latest logs from the OpenSearch server

  docker-compose logs opensearch-dashboard
                Outputs latest logs from the OpenSearch Dashboard server

  docker-compose down
                Shut down and clean up both containers.

  docker-compose up -d
                Bring both containers back up.

You can clean up all Docker containers and any execution plans created (if
applicable) by running
  $(pwd)/cleanup_resources.sh

The full text of this message is also available at
  $(pwd)/README
EOF
cat README
