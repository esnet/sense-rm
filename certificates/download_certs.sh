#!/bin/bash

# List of hosts and port numbers (in the format "hostname:port")
hosts=( "nsi0.uhnet.net:9443" \
        "prod-nsi.geant.org:9043" \
        "oscars-prod.es.net:443" \
        "nsi0.snvaca.pacificwave.net:9443" \
        "nsi0.sttlwa.pacificwave.net:9443" \
        "nsi0.lsanca.pacificwave.net:9443" \
        "nsi0.tokyjp.pacificwave.net:9443" \
        "aggr.nsi.nii.ac.jp:28443" \
        "nsi.ampath.net:9443" \
        "raw.githubusercontent.com:443" \
        "opennsa.nsi.nrp-nautilus.io:443" \
        "oscars-testbed.es.net:443" \
        "nsi-aggr-west.es.net:443" \
        "opennsa.canarie.ca:9443" \
        "dds.nsi.nrp-nautilus.io:443" \
        "safnari.nsi.nrp-nautilus.io:443" \
        "prod-nsi.geant.org:8401" \
        "sense-rm.es.net:8000" \
        "dds.dlp.surfnet.nl:443" \
        "agg.netherlight.net:443" \
        "dds.netherlight.net:443" \
        "opennsa.northwestern.edu:9443" \
        "sense-o.es.net:8443" \
        "sense-rm.es.net:443" \
      )
        # "nsi.twaren.net:9443" \
        # "idc.cipo.rnp.br:443" \
        # "agg.cipo.rnp.br:443" \
        # "daej-nsi.kreonet.net:9443" \
        # "ns.ps.jgn-x.jp:443" \
	# "supa.dlp.surfnet.nl:443" \

# Loop through the list of hosts
rm -f tmp.1
for host in "${hosts[@]}"; do
  # Extract hostname and port
  IFS=":" read -r hostname port <<< "$host"

  # Generate the filename for the certificate
  filename="${hostname//./_}.pem"

  # Download the certificate using OpenSSL
  echo "Downloading ${hostname}:${port} ..."
  if openssl s_client -connect "${hostname}:${port}" -showcerts -servername "${hostname}" </dev/null 2>/dev/null | openssl x509 -out "${filename}"; then
    subject=`openssl x509 -noout -subject -in "${filename}" | cut -f2- -d'=' | awk '{$1=$1};1'`
    issuer=`openssl x509 -noout -issuer -in "${filename}" | cut -f2- -d'=' | awk '{$1=$1};1'`
    validity=`openssl x509 -noout -dates -in "${filename}"`

    echo " 0 s:${subject}" > tmp.1
    echo "   i:${issuer}" >> tmp.1
    echo "Validity:" >> tmp.1
    echo "${validity}" | sed -e 's/^/   /' -  >> tmp.1
    cat "${filename}" >> tmp.1
    mv tmp.1 "${filename}"
    echo "Certificate for $hostname saved to $filename"
  else
    echo "Failed to download certificate for $hostname"
  fi
done
