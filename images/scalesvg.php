<?php
/**
 * scale SVG image
 *
 * there is a better way to scal images, see scal-icons instead
 * but it does not work :(
 */


class SvgScaler {
    private $doc;

    function __construct($filename=''){
        if ($filename){
            $this->load($filename);
        }
    }

    function load($filename){
        $this->doc = new DOMDocument("1.1");
        $this->doc->load($filename);
    }

    function save($filename){
        $this->doc->save($filename);
    }

    function scale($k){
        if ($k==0) return; //do not scale
        //iterate trough elements and scale values
        $root = $this->doc->documentElement;
        $this->scaleElement($root,$k);
    }

    protected function scaleElement($el,$k){
        //print $el->nodeName;

        foreach(array('width','height','x1','x2','y1','y2','cx','cy','fx','fy','r','rx','ry','x','y') as $name){
            if ($el->hasAttribute($name)){
                $el->setAttribute($name,$this->scaleValue($el->getAttribute($name),$k));
            }
        }

        foreach(array('d') as $name){
            if ($el->hasAttribute($name)){
                $el->setAttribute($name,$this->scalePath($el->getAttribute($name),$k));
            }
        }


        foreach(array('transform','gradientTransform') as $name){
            if ($el->hasAttribute($name)){
                $el->setAttribute($name,$this->scaleTransform($el->getAttribute($name),$k));
            }
        }

        foreach(array('style') as $name){
            if ($el->hasAttribute($name)){
                $el->setAttribute($name,$this->scaleStyle($el->getAttribute($name),$k));
            }
        }




        for($item=$el->firstChild;$item;$item = $item->nextSibling){
            if ($item->nodeType==1){
                $this->scaleElement($item,$k);
            }
        }
    }

    protected function scaleStyle($value,$k){
        $components = explode(';',$value);
        $components1 = array();
        foreach($components as $key=>$value){
            if (empty($value)) continue;
            //print "\n".$value.";\n";
            list($name,$v) = explode(':',$value);
            if ($name=='stroke-width'){
                $v = $v*$k;
            }
            $components1[] = $name.':'.$v;
        }
        return implode(';',$components1);
    }

    protected function scalePath($value,$k){
        $components = explode(' ',$value);
        $components_new = array();
        foreach($components as $key=>$v){
            if (($pos = strpos($v,',')) !==FALSE){
                list($x,$y) = explode(',',$v);
                $new_value = ($x*$k).','.($y*$k);
                $components_new[$key] = $new_value;
            }
            else {
                $components_new[$key] = $v;
            }
        }
        return implode(' ',$components_new);
    }

    protected function scaleTransform($value,$k){
        if (strpos($value,'translate(')===0) return $this->scaleTranslate($value,$k);
        if (strpos($value,'matrix(')===0) return $this->scaleMatrix($value,$k);
        return $value;
    }

    protected function scaleTranslate($value,$k){
        $value = substr($value,10,-1);
        $components = explode(',',$value);
        $components[0] = $components[0]*$k;
        $components[1] = $components[1]*$k;
        return 'translate('.implode(',',$components).')';
    }

    protected function scaleMatrix($value,$k){

        $value1 = substr($value,7,-1);
        $components = explode(',',$value1);

        $components[4] = $components[4]*$k;
        $components[5] = $components[5]*$k;
        return 'matrix('.implode(',',$components).')';
    }



    protected function scaleValue($value,$k){
        //get inits
        if (($pos = strpos('px',$value))!==false){
            $numvalue = substr(0,-2); //cut off value
            return ($numvalue*$k).'px';
        }
        else { //numeric value
            return ($value*$k);
        }
    }
}


class androidResizer {
    var $modes = array(
        'ldpi' => 0.75,
        'mdpi' => 1,
        'hdpi' => 1.5,
        'xhdpi' => 2,
        'xxhdpi' => 3,
        'xxxhdpi' => 4
    );


    /**
     * resize for all possible densities
     * @param unknown_type $name
     */
    function resize($name, $scale = 1){
        foreach($this->modes as $mode => $k) {
            $scaledSvgFile = __DIR__ . '/scaled/'.$mode.'_'.$name.'.svg';
            $s = new SvgScaler(__DIR__ . '/icons/' . $name . '.svg');
            $s->scale($k / $scale);
            $s->save($scaledSvgFile);

            //now resterize
            system('java -jar ~/bin/batik-1.11/batik-rasterizer-1.11.jar '. $scaledSvgFile);
            copy(__DIR__ . '/scaled/'.$mode.'_'.$name.'.png', __DIR__ . '/scaled/drawable-' . $mode . '/' . $name . '.png');
        }
    }

    /**
     * Resize all icons used on app and put into folders as requested
     *
     */
    public function resizeAllIcons() {
        $this->resize('icon');
        $this->resize('settings');
        $this->resize('documentsave');
        $this->resize('documentopen');
        $this->resize('documentnew');
        $this->resize('documentsave_as');
        $this->resize('file');
        $this->resize('folder');
        $this->resize('editfind');
    }

    /**
     * resize raster images
     *
     * @param unknown_type $name
     */
    function resizeImage($name){
        foreach($this->modes as $mode=>$k){
            $i = new IMagick($name.'.png');
            $k1 = $k/2;
            if ($k1!=1){
                $i->thumbnailImage($i->getimagewidth()*$k1,$i->getimageheight()*$k1);
            }

            file_put_contents('../res/drawable-'.$mode.'/'.$name.'.png',$i->getImageBLob());
        }

    }
}

$r = new androidResizer();
$r->resizeAllIcons();
/*
$r->resize('icon');
$r->resize('settings',1.5);
$r->resize('documentsave',1.5);
$r->resize('documentopen',1.5);
$r->resize('documentnew',1.5);
$r->resize('documentsave_as',1.5);
$r->resize('file',2);
$r->resize('folder',2);*/
//$r->resize('editfind',1.5);

//$s = new SvgScaler('icon.svg');
//$s->scale(5);
//$s->save('resized/icon200.svg');
//now resterize
//system('java -jar ~/software/batik/batik-rasterizer.jar '.'resized/icon200.svg');



/*
$r->resize('bluetooth_off');
$r->resize('bluetooth_phone');
$r->resize('ic_launcher');
$r->resizeImage('jabra');
$r->resizeImage('jabra_on');
*/


/*
            $s = new SvgScaler('bluetooth_1.svg');
            $s->scale(30);
            $s->save('bluetooth_2.svg');

            //now resterize
            system('java -jar ~/software/batik/batik-rasterizer.jar '.'bluetooth_2.svg');
*/
