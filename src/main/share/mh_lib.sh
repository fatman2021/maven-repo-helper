# This is included by mh_* and contains common functions
#
# - argument parsing
# - package selection
#

MH_VERSION=${MH_VERSION:-0.6}
CLASSPATH=/usr/share/java/stax-api.jar:/usr/share/java/stax.jar:/usr/share/java/xml-apis.jar:/usr/share/java/maven-repo-helper.jar
JAVA_OPTIONS=-Djavax.xml.stream.XMLOutputFactory=com.bea.xml.stream.XMLOutputFactoryBase

parseargs()
{
   ARGC=0
   ARGV=()
   while [ -n "$1" ]; do
      if [ "-V" = "$1" ] || [ "--version" = "$1" ]; then
         echo "Maven Repo Helper version $MH_VERSION"
         exit 0
      elif [ "-h" = "$1" ] || [ "--help" = "$1" ]; then
         syntax
      elif [ "-" = "${1:1:1}" ]; then
      # long opt
         optn=$(echo $1 | sed -n 's/^--\([^=]*\)\(=.*\)*$/\1/p' | sed 's/-/_/g')
         if [ -z "$optn" ] || ! echo $ARGS | sed 's/-/_/g' | grep $optn >/dev/null; then
            echo "Invalid option: $optn"
            syntax
         fi
         optv=$(echo $1 | sed -n 's/^[^=]*=\(.*\)$/\1/p')
         if [ -z "$optv" ]; then
            optv=true
         fi
         export opt_$optn="$optv"
      elif [ "-" = "${1:0:1}" ]; then
      # short opt
         optn="${1:1:1}"
         if [ -z "$optn" ] || ! echo $ARGS | sed 's/-/_/g' | grep $optn >/dev/null; then
            echo "Invalid option: $optn"
            syntax
         fi
         optv="${1:2}"
         if [ -z "$optv" ]; then
            optv=true
         fi
         export opt_$optn="$optv"
      else
      # not-opt arg
         ARGV[$ARGC]="$1"
         ARGC=$(( $ARGC + 1 ))
      fi
      shift
   done
   export ARGC
   export ARGV
}

getarg()
{
   while [ -n "$1" ]; do
      optn=$(echo "opt_$1" | sed 's/-/_/g')
      if [ -n "${!optn}" ]; then
         echo ${!optn}
         return
      fi
      shift
   done
}
