#!/usr/bin/php
<?
error_reporting(-1);
echo "get all files with text constans:\n";


class Checker {
	static $ignore_strings = array("deprecation");
	static $ignore_files = array('TPStrings.java','Settings.java');
	function run(){
		$this->doPath(dirname(dirname(__FILE__))."/src");
	}
	
	function doPath($path){
		$dir = opendir($path);
		while($file = readdir($dir)){
			if (strpos($file,".")===0) continue;
			$filepath = $path."/".$file;
			if (is_dir($filepath)){
				$this->doPath($filepath);
			}
			if (is_file($filepath)){
				if (in_array($file,self::$ignore_files)) continue;
				print "File:".$file."\n";
				$content = file_get_contents($filepath);
				preg_match_all("/\"([^\"\\\]*(\\\.[^\"\\\]*)*)\"/",$content,$res);
				if (!isset($res[0])) continue;
				foreach($res[0] as $r){
					if (in_array($r,self::$ignore_strings)) continue;					
					print $r."\n";
				}
			}
		}
		closedir($dir);		
	}
	
}


$c = new Checker();
$c->run();
