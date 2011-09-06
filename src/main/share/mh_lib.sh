# This is included by mh_* and contains common functions
#
# - argument parsing
# - package selection
#

MH_VERSION=${MH_VERSION:-1.6}
CLASSPATH=/usr/share/java/stax-api.jar:/usr/share/java/stax.jar:/usr/share/java/xml-apis.jar:/usr/share/java/maven-repo-helper.jar
JAVA_OPTIONS="-Djavax.xml.stream.XMLOutputFactory=com.bea.xml.stream.XMLOutputFactoryBase -Djavax.xml.stream.XMLInputFactory=com.bea.xml.stream.MXParserFactory"

parseargs()
{
   # clean arguments from a previous run
   unset "${!opt_@}"

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
         optn=$(echo "$1" | sed -n 's/^--\([^=]*\)\(=.*\)*$/\1/p' | sed 's/-/_/g')
         if [ -z "$optn" ] || ! echo $ARGS | sed 's/-/_/g' | grep $optn >/dev/null; then
            echo "Invalid option: $optn"
            syntax
         fi
         optv=$(echo "$1" | sed -n 's/^[^=]*=\(.*\)$/\1/p')
         if [ -z "$optv" ]; then
            optv=true
         fi
         export -a opt_$optn
         eval opt_$optn'+=("$optv")'
      elif [ "-" = "${1:0:1}" ]; then
      # short opt
         optn="${1:1:1}"
         if [ "$optn" = "O" ]; then
            : # skip -O options passed by dh 7
         elif [ -z "$optn" ] || ! echo $ARGS | sed 's/-/_/g' | grep $optn >/dev/null; then
            echo "Invalid option: $optn"
            syntax
         else
            optv="${1:2}"
            if [ -z "$optv" ]; then
               optv=true
            fi
            export -a opt_$optn
            eval opt_$optn'+=("$optv")'
         fi
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
      if [ -n "${!optn[0]}" ]; then
         echo ${!optn[0]}
         return
      fi
      shift
   done
}

getargs()
{
   echo -n '('
   while [ -n "$1" ]; do
      optn=$(echo "opt_$1" | sed 's/-/_/g')
      if [ -n "${!optn[0]}" ]; then
         echo -n '"${'$optn'[@]}" '
      fi
      shift
   done
   echo ')'
}

findpackages()
{
   if [ -n "$opt_p" ]; then
      echo $opt_p
   elif [ -n "$opt_package" ]; then
      echo $opt_package
   elif [ -n "$opt_i" ] || [ -n "$opt_indep" ]; then
      egrep '^(Package|Architecture)' debian/control | grep -B1 'Architecture: all'|sed -n '/^Package:/s/^[^:]*: *//p'
   elif [ -n "$opt_a" ] || [ -n "$opt_arch" ]; then
      egrep '^(Package|Architecture)' debian/control | grep -v 'Architecture: all' | grep -B1 Architecture|sed -n '/^Package:/s/^[^:]*: *//p'
   else
      sed -n '/^Package:/s/^[^:]*: *//p' debian/control
   fi
}

firstpackage()
{ 
   findpackages | head -n1
}

