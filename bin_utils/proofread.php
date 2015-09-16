#!/usr/bin/php
<?
error_reporting(-1);
echo "all language constants:\n";


class Checker {
	
	function run(){
		//read english files
		$filename = dirname(dirname(__FILE__))."/res/values/strings.xml";
		
		//read english files
		$filename_ru = dirname(dirname(__FILE__))."/res/values-ru/strings.xml";
		
		$english = $this->getStrings($filename);

		$russian = $this->getStrings($filename_ru);
		
		print "==============================Russian==============================\n";
		foreach($english as $key=>$value){
			print $key.": ".$value."\n";
		}
		
		print "=============================English===============================\n";
		foreach($russian as $key=>$value){
			print $key.": ".$value."\n";
		}
	}
	
	function getStrings($filename){
		$dom = new DOMDocument('1.0', 'UTF8');
		$dom->load('file://'.$filename);
		$strings = array();
		$el = $dom->documentElement->firstChild;
		//$info->url = $doc->documentElement->getAttribute('url');
		while($el){
			if (($el->nodeType==XML_ELEMENT_NODE) && ($el->nodeName=='string')){
				$strings[$el->getAttribute('name')] = $el->nodeValue;
			}
			$el = $el->nextSibling;
		}
		return $strings;			
	}
}


$c = new Checker();
$c->run();
