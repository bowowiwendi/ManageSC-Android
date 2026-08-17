package com.managesc.app

object Constants {
    /** Skrip setup VPS otomatis (WendyVpn). Dijalankan via SSH. */
    const val SETUP_SCRIPT = """sysctl net.ipv6.conf.all.disable_ipv6=1 && sysctl net.ipv6.conf.default.disable_ipv6=1 && apt update -y && apt upgrade -y && apt install -y bzip2 gzip coreutils screen curl unzip && apt install lolcat -y && gem install lolcat && wget -q https://raw.githubusercontent.com/bowowiwendi/WendyVpn/ABSTRAK/setup-main.sh && chmod +x setup-main.sh && sed -i -e 's/\$//' setup-main.sh && screen -S setupku ./setup-main.sh"""
}
