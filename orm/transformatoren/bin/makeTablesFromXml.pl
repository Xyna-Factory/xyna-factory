#!/usr/bin/perl -w

use strict;

my $tmp = `ls *.xml`;
my @xmlFiles = split("\n", $tmp);
my $out;
foreach (@xmlFiles) {
    my $command = "transform -IN $_ -XSL $ARGV[0] -Q";
    $out = `$command`;
    $out = "$out\n";

    #$out =~ s/ +\n//g;
    print "$out";
} 

