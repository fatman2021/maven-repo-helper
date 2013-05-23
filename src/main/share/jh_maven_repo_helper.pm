#!/usr/bin/perl                                                                                                                
use warnings;
use strict;
use Debian::Debhelper::Dh_Lib;

# To use with javahelper
# dh $@ --with javahelper --with jh_mavenrepohelper

insert_after("jh_depends", "mh_installpoms");
insert_after("mh_installpoms", "mh_linkjars");
add_command_options("mh_linkjars", "--skip-clean-poms");
insert_before("dh_clean", "mh_clean");

1;
