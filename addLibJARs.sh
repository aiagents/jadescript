find . -type d -name "libs" -exec cp -v Jadescript/it.unipr.ailab.jadescript.lib/outJar/jadescript.jar {} \;

echo "Removing cyclic jars:"
rm -v -f Jadescript/it.unipr.ailab.jadescript.lib/libs/jadescript.jar


find . -type d -name "libs" -exec cp -v Jadescript/it.unipr.ailab.jadescript.javaapi/outJar/jadescriptjavaapi.jar {} \;

echo "Removing cyclic jars:"
rm -v -f Jadescript/it.unipr.ailab.jadescript.lib/libs/jadescriptjavaapi.jar
rm -v -f Jadescript/it.unipr.ailab.jadescript.javaapi/libs/jadescriptjavaapi.jar