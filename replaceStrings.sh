#!/bin/bash

# This script replaces Wikipedia with DroidWiki in all language strings (not in code)

# cd to res directory
cd app/main/src/res
# replace wp_stylized string
find ./values* -type f -print0 | xargs -0 sed -i -e 's!<string name="wp_stylized">.*</string>!<string name="wp_stylized">\&lt;big\&gt;D\&lt;/big\&gt;ROIDWIK\&lt;big\&gt;I\&lt;/big\&gt;</string>!g'

# replace Wikipedia -> DroidWiki
find ./values* -type f -print0 | xargs -0 sed -i 's/Wikipedia/DroidWiki/g'
