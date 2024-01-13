#!/bin/bash

set -o errexit
set -o errtrace
set -o pipefail
set -o nounset

# Some useful constants
readonly DOCKER_IMAGE_TAG="opensearch-with-personalized-search-ranking"
readonly OPENSEARCH_VERSION="2.9.0"

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
  SEARCH_PROCESSOR_PLUGIN_URL="https://github.com/opensearch-project/search-processor/releases/download/${OPENSEARCH_VERSION}/opensearch-search-processor-${OPENSEARCH_VERSION}.0.zip"
fi

function print_help() {
  cat << EOF
Usage: $0 [-r <region>] [--profile <AWS profile name>]
        [--volume-name <docker_volume_name>]
  -r | --region                     The AWS region for the Personalize Intelligent Ranking
                                    service endpoint. If not specified, will read from the
                                    AWS CLI for the default profile.
  --profile                         The AWS profile to use for credentials. If not set, then
                                    the script will try first to use credentials from the
                                    environment, then from the default AWS profile.
  --volume-name                     Without this option, the OpenSearch container will write
                                    the index to ephemeral container storage, which is lost when
                                    the container is removed. Using this option will map the
                                    named Docker volume to \$OPENSEARCH_ROOT/data, so index data
                                    will persist across executions. If the named volume does not
                                    exist, it will be created.

  NOTE: If the --profile option is not specified, the script will attempt to read AWS
  credentials (access/secret key, optional session token) from environment variables,
  and then from the default AWS profile, in order to pass them to the OpenSearch keystore
  to be used to connect to the Personalize Intelligent ranking service.
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
      -r | --region )
        shift
        AWS_REGION=$1
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
      --volume-name )
        shift
        VOLUME_NAME=$1
        shift
        ;;
        esac
done

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
      if [ -z "${AWS_ACCESS_KEY_ID:-}" ] || [ -z "${AWS_SECRET_ACCESS_KEY}" ]; then
        echo "Unable to load credentials from default profile. No credentials will be passed to the OpenSearch keystore."
        echo "OpenSearch will use the default credential provider chain to access Personalize, which may rely on EC2 instance"
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
      if [ -n "${AWS_SESSION_TOKEN:-}" ]; then
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

echo "Established AWS key id, secret key, session token and region."

#
# Create a unique directory to hold the Dockerfile and docker-compose.yml files.
#
PLATFORM=$(uname)
if [ "${PLATFORM}" == "Darwin" ]; then
  DOCKER_BUILD_DIR=$(mktemp -d opensearch-personalize-intelligent-ranking-docker.XXXX)
else
  # Assume GNU mktemp
  DOCKER_BUILD_DIR=$(mktemp -d -p . opensearch-personalize-intelligent-ranking-docker.XXXX)
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
echo "Dockerfile created"
#
# If credentials were resolved above, push them to the OpenSearch keystore in the Docker image.
#
if [ -n "${AWS_ACCESS_KEY_ID:-}" ] && [ -n "${AWS_SECRET_ACCESS_KEY:-}" ]; then
  touch personalized_search_ranking.credentials
  chmod 600 personalized_search_ranking.credentials
  cat >>personalized_search_ranking.credentials <<EOF
personalized_search_ranking.aws.access_key:$AWS_ACCESS_KEY_ID
personalized_search_ranking.aws.secret_key:$AWS_SECRET_ACCESS_KEY
EOF
  if [ -n "${AWS_SESSION_TOKEN:-}" ]; then
    # If a session token was found in the environment / profile, push it to the OpenSearch keystore too.
    echo "WARNING: Adding session token to OpenSearch keystore. These temporary credentials will"
    echo "expire, and should only be used for short-lived testing."
    cat >>personalized_search_ranking.credentials <<EOF
personalized_search_ranking.aws.session_token:$AWS_SESSION_TOKEN
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
COPY --chown=opensearch:opensearch personalized_search_ranking.credentials /tmp
RUN /usr/share/opensearch/bin/opensearch-keystore create
RUN --mount=type=secret,id=credentials,target=/tmp/personalized_search_ranking.credentials,required=true,mode=0444 \
  /tmp/install_credentials.sh /tmp/personalized_search_ranking.credentials
EOF
fi
echo "Opensearch credentials saved"
#
# Build and tag the Docker image with the plugin (and maybe credentials in the keystore)
#
if [ -f personalized_search_ranking.credentials ]; then
  DOCKER_BUILDKIT=1 docker build --tag ${DOCKER_IMAGE_TAG} --secret id=credentials,src=personalized_search_ranking.credentials .
  rm personalized_search_ranking.credentials
else
  docker build --tag ${DOCKER_IMAGE_TAG} .
fi
echo "Docker image built and tagged with credentials"
#
# Make sure we have opensearch-dashboards:
#
docker pull ${OPENSEARCH_DASHBOARDS_IMAGE_TAG}
echo "Docker image pulled"

if [ -n "${VOLUME_NAME:-}" ]; then
  if ! docker volume inspect ${VOLUME_NAME}> /dev/null; then
    echo "Creating volume ${VOLUME_NAME}";
    docker volume create ${VOLUME_NAME}
  fi
  DATA_DIR_BLOCK="    volumes:
      - ${VOLUME_NAME}:/usr/share/opensearch/data"
  VOLUME_BLOCK="volumes:
  ${VOLUME_NAME}:
    external: true"
fi
echo "Volume created"

# Starting in 2.12.0, security demo configuration script requires an initial admin password
OPENSEARCH_REQUIRED_VERSION="2.12.0"
COMPARE_VERSION=`echo $OPENSEARCH_REQUIRED_VERSION $OPENSEARCH_VERSION | tr ' ' '\n' | sort -V | uniq | head -n 1`
if [ "$COMPARE_VERSION" != "$OPENSEARCH_REQUIRED_VERSION" ]; then
  OPENSEARCH_INITIAL_ADMIN_PASSWORD="admin"
else
  OPENSEARCH_INITIAL_ADMIN_PASSWORD="myStrongPassword123!"
fi

#
# Create a docker-compose.yml file that will launch an OpenSearch node with the image we
# just built and an OpenSearch Dashboards node that points to the OpenSearch node.
#
cat >docker-compose.yml <<EOF
version: '3'
networks:
  opensearch-net:
${VOLUME_BLOCK:-}
services:
  opensearch-node:
    image: ${DOCKER_IMAGE_TAG}
    container_name: opensearch-node
    environment:
      - cluster.name=opensearch-cluster
      - node.name=opensearch-node
      - discovery.type=single-node
      - OPENSEARCH_INITIAL_ADMIN_PASSWORD=${OPENSEARCH_INITIAL_ADMIN_PASSWORD}
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
${DATA_DIR_BLOCK:-}
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

echo "docker-compose.yml created"
#
# Bring up the images
#
docker-compose up -d

echo "Docker compose up executed, docker should be up."

cat >cleanup_resources.sh <<EOF
#!/bin/bash
echo "This script will shut down and clean up running Docker containers for OpenSearch and OpenSearch Dashboards."

RESPONSE="unknown"
while ! [ "\${RESPONSE}" == "yes" ] && ! [ "\${RESPONSE}" == "no" ] && ! [ "\${RESPONSE}" == "n" ] \
&& ! [ "\${RESPONSE}" == "y" ]; do
  echo "Are you sure? (yes/no)"
  read RESPONSE
done

if [ "\${RESPONSE}" == "yes" || "\${RESPONSE}" == "y" ]; then
  cd \$(dirname \$0)
  docker-compose down
fi
EOF
chmod +x cleanup_resources.sh

echo "Cleanup file created"

#
# Output helpful "getting started" text.
#
cat >README <<EOF

OpenSearch container launched, listening on port 9200.
OpenSearch Dashboards container launched, listening on port 5601.

Interact with OpenSearch using curl by authenticating as admin like:
  curl -ku "admin:<admin-password>" https://localhost:9200/

Index some data on OpenSearch by following instructions at
https://opensearch.org/docs/latest/opensearch/index-data/


Connect to OpenSearch Dashboards with a web browser at http://localhost:5601/,
using username admin and password admin. Select "Search Relevance" from the
top-left menu. In the resulting UI, you can submit a query without Personalized
search ranking and one with Personalized search Ranking.

To configure and setup Personalize search ranking, run a curl command as follows:

curl -X PUT "https://localhost:9200/_search/pipeline/intelligent_ranking" -u 'admin:<admin-password>' --insecure -H 'Content-Type: application/json' -d'
{
  "description": "A pipeline to apply custom reranking",
  "response_processors" : [
    {
      "personalized_search_ranking" : {
        "campaign_arn" : "<personalize_campaign_arn>",
        "item_id_field" : "<item_id_field>",
        "recipe" : "<recipe>",
        "weight" : "<weight>",
        "iam_role_arn": "<iam-role-arn-created-earlier>",
        "aws_region": "<region>"
      }
    }
  ]
}'

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