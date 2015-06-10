#!/usr/bin/perl                                                                                                                
use warnings;
use strict;
use Debian::Debhelper::Dh_Lib;

# dh $@ --with maven_repo_helper

insert_after("dh_install", "mh_install");
insert_before("dh_clean", "mh_clean");

1;
