package jadescript.lang;

import jade.content.AgentAction;

@SuppressWarnings({"rawtypes", "unused"})
public interface Tuple extends AgentAction {
    int getLength();

    class Tuple0 implements Tuple{

        public Tuple0(){

        }

        @Override
        public int getLength() {
            return 0;
        }

        @Override
        public String toString() {
            return "()";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple0){
                Tuple0 t = (Tuple0) o;
                return true;
            }
            return super.equals(o);
        }


    }

    class Tuple1<T0> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        public Tuple1() {
        }

        public Tuple1(T0 _0){

            this.element0 = _0;

        }

        @Override
        public int getLength() {
            return 1;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple1){
                Tuple1 t = (Tuple1) o;
                return this.getElement0().equals(t.getElement0());
            }
            return super.equals(o);
        }


    }

    class Tuple2<T0, T1> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        public Tuple2() {
        }

        public Tuple2(T0 _0, T1 _1){

            this.element0 = _0;

            this.element1 = _1;

        }

        @Override
        public int getLength() {
            return 2;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple2){
                Tuple2 t = (Tuple2) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1());
            }
            return super.equals(o);
        }


    }

    class Tuple3<T0, T1, T2> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        public Tuple3() {
        }

        public Tuple3(T0 _0, T1 _1, T2 _2){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

        }

        @Override
        public int getLength() {
            return 3;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple3){
                Tuple3 t = (Tuple3) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2());
            }
            return super.equals(o);
        }


    }

    class Tuple4<T0, T1, T2, T3> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        public Tuple4() {
        }

        public Tuple4(T0 _0, T1 _1, T2 _2, T3 _3){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

        }

        @Override
        public int getLength() {
            return 4;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple4){
                Tuple4 t = (Tuple4) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3());
            }
            return super.equals(o);
        }


    }

    class Tuple5<T0, T1, T2, T3, T4> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        public Tuple5() {
        }

        public Tuple5(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

        }

        @Override
        public int getLength() {
            return 5;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple5){
                Tuple5 t = (Tuple5) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4());
            }
            return super.equals(o);
        }


    }

    class Tuple6<T0, T1, T2, T3, T4, T5> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        private T5 element5;
        public T5 getElement5(){
            return this.element5;
        }
        public void setElement5(T5 value){
            this.element5 = value;
        }


        public Tuple6() {
        }

        public Tuple6(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4, T5 _5){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

            this.element5 = _5;

        }

        @Override
        public int getLength() {
            return 6;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+ "," +
                    (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple6){
                Tuple6 t = (Tuple6) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4()) && this.getElement5().equals(t.getElement5());
            }
            return super.equals(o);
        }


    }

    class Tuple7<T0, T1, T2, T3, T4, T5, T6> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        private T5 element5;
        public T5 getElement5(){
            return this.element5;
        }
        public void setElement5(T5 value){
            this.element5 = value;
        }


        private T6 element6;
        public T6 getElement6(){
            return this.element6;
        }
        public void setElement6(T6 value){
            this.element6 = value;
        }


        public Tuple7() {
        }

        public Tuple7(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

            this.element5 = _5;

            this.element6 = _6;

        }

        @Override
        public int getLength() {
            return 7;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+ "," +
                    (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5 )+ "," +
                    (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple7){
                Tuple7 t = (Tuple7) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4()) && this.getElement5().equals(t.getElement5()) && this.getElement6().equals(t.getElement6());
            }
            return super.equals(o);
        }


    }

    class Tuple8<T0, T1, T2, T3, T4, T5, T6, T7> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        private T5 element5;
        public T5 getElement5(){
            return this.element5;
        }
        public void setElement5(T5 value){
            this.element5 = value;
        }


        private T6 element6;
        public T6 getElement6(){
            return this.element6;
        }
        public void setElement6(T6 value){
            this.element6 = value;
        }


        private T7 element7;
        public T7 getElement7(){
            return this.element7;
        }
        public void setElement7(T7 value){
            this.element7 = value;
        }


        public Tuple8() {
        }

        public Tuple8(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

            this.element5 = _5;

            this.element6 = _6;

            this.element7 = _7;

        }

        @Override
        public int getLength() {
            return 8;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+ "," +
                    (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5 )+ "," +
                    (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6 )+ "," +
                    (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple8){
                Tuple8 t = (Tuple8) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4()) && this.getElement5().equals(t.getElement5()) && this.getElement6().equals(t.getElement6()) && this.getElement7().equals(t.getElement7());
            }
            return super.equals(o);
        }


    }

    class Tuple9<T0, T1, T2, T3, T4, T5, T6, T7, T8> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        private T5 element5;
        public T5 getElement5(){
            return this.element5;
        }
        public void setElement5(T5 value){
            this.element5 = value;
        }


        private T6 element6;
        public T6 getElement6(){
            return this.element6;
        }
        public void setElement6(T6 value){
            this.element6 = value;
        }


        private T7 element7;
        public T7 getElement7(){
            return this.element7;
        }
        public void setElement7(T7 value){
            this.element7 = value;
        }


        private T8 element8;
        public T8 getElement8(){
            return this.element8;
        }
        public void setElement8(T8 value){
            this.element8 = value;
        }


        public Tuple9() {
        }

        public Tuple9(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

            this.element5 = _5;

            this.element6 = _6;

            this.element7 = _7;

            this.element8 = _8;

        }

        @Override
        public int getLength() {
            return 9;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+ "," +
                    (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5 )+ "," +
                    (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6 )+ "," +
                    (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7 )+ "," +
                    (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple9){
                Tuple9 t = (Tuple9) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4()) && this.getElement5().equals(t.getElement5()) && this.getElement6().equals(t.getElement6()) && this.getElement7().equals(t.getElement7()) && this.getElement8().equals(t.getElement8());
            }
            return super.equals(o);
        }


    }

    class Tuple10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        private T5 element5;
        public T5 getElement5(){
            return this.element5;
        }
        public void setElement5(T5 value){
            this.element5 = value;
        }


        private T6 element6;
        public T6 getElement6(){
            return this.element6;
        }
        public void setElement6(T6 value){
            this.element6 = value;
        }


        private T7 element7;
        public T7 getElement7(){
            return this.element7;
        }
        public void setElement7(T7 value){
            this.element7 = value;
        }


        private T8 element8;
        public T8 getElement8(){
            return this.element8;
        }
        public void setElement8(T8 value){
            this.element8 = value;
        }


        private T9 element9;
        public T9 getElement9(){
            return this.element9;
        }
        public void setElement9(T9 value){
            this.element9 = value;
        }


        public Tuple10() {
        }

        public Tuple10(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8, T9 _9){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

            this.element5 = _5;

            this.element6 = _6;

            this.element7 = _7;

            this.element8 = _8;

            this.element9 = _9;

        }

        @Override
        public int getLength() {
            return 10;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+ "," +
                    (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5 )+ "," +
                    (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6 )+ "," +
                    (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7 )+ "," +
                    (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8 )+ "," +
                    (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple10){
                Tuple10 t = (Tuple10) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4()) && this.getElement5().equals(t.getElement5()) && this.getElement6().equals(t.getElement6()) && this.getElement7().equals(t.getElement7()) && this.getElement8().equals(t.getElement8()) && this.getElement9().equals(t.getElement9());
            }
            return super.equals(o);
        }


    }

    class Tuple11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        private T5 element5;
        public T5 getElement5(){
            return this.element5;
        }
        public void setElement5(T5 value){
            this.element5 = value;
        }


        private T6 element6;
        public T6 getElement6(){
            return this.element6;
        }
        public void setElement6(T6 value){
            this.element6 = value;
        }


        private T7 element7;
        public T7 getElement7(){
            return this.element7;
        }
        public void setElement7(T7 value){
            this.element7 = value;
        }


        private T8 element8;
        public T8 getElement8(){
            return this.element8;
        }
        public void setElement8(T8 value){
            this.element8 = value;
        }


        private T9 element9;
        public T9 getElement9(){
            return this.element9;
        }
        public void setElement9(T9 value){
            this.element9 = value;
        }


        private T10 element10;
        public T10 getElement10(){
            return this.element10;
        }
        public void setElement10(T10 value){
            this.element10 = value;
        }


        public Tuple11() {
        }

        public Tuple11(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8, T9 _9, T10 _10){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

            this.element5 = _5;

            this.element6 = _6;

            this.element7 = _7;

            this.element8 = _8;

            this.element9 = _9;

            this.element10 = _10;

        }

        @Override
        public int getLength() {
            return 11;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+ "," +
                    (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5 )+ "," +
                    (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6 )+ "," +
                    (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7 )+ "," +
                    (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8 )+ "," +
                    (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9 )+ "," +
                    (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple11){
                Tuple11 t = (Tuple11) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4()) && this.getElement5().equals(t.getElement5()) && this.getElement6().equals(t.getElement6()) && this.getElement7().equals(t.getElement7()) && this.getElement8().equals(t.getElement8()) && this.getElement9().equals(t.getElement9()) && this.getElement10().equals(t.getElement10());
            }
            return super.equals(o);
        }


    }

    class Tuple12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        private T5 element5;
        public T5 getElement5(){
            return this.element5;
        }
        public void setElement5(T5 value){
            this.element5 = value;
        }


        private T6 element6;
        public T6 getElement6(){
            return this.element6;
        }
        public void setElement6(T6 value){
            this.element6 = value;
        }


        private T7 element7;
        public T7 getElement7(){
            return this.element7;
        }
        public void setElement7(T7 value){
            this.element7 = value;
        }


        private T8 element8;
        public T8 getElement8(){
            return this.element8;
        }
        public void setElement8(T8 value){
            this.element8 = value;
        }


        private T9 element9;
        public T9 getElement9(){
            return this.element9;
        }
        public void setElement9(T9 value){
            this.element9 = value;
        }


        private T10 element10;
        public T10 getElement10(){
            return this.element10;
        }
        public void setElement10(T10 value){
            this.element10 = value;
        }


        private T11 element11;
        public T11 getElement11(){
            return this.element11;
        }
        public void setElement11(T11 value){
            this.element11 = value;
        }


        public Tuple12() {
        }

        public Tuple12(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8, T9 _9, T10 _10, T11 _11){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

            this.element5 = _5;

            this.element6 = _6;

            this.element7 = _7;

            this.element8 = _8;

            this.element9 = _9;

            this.element10 = _10;

            this.element11 = _11;

        }

        @Override
        public int getLength() {
            return 12;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+ "," +
                    (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5 )+ "," +
                    (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6 )+ "," +
                    (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7 )+ "," +
                    (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8 )+ "," +
                    (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9 )+ "," +
                    (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10 )+ "," +
                    (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple12){
                Tuple12 t = (Tuple12) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4()) && this.getElement5().equals(t.getElement5()) && this.getElement6().equals(t.getElement6()) && this.getElement7().equals(t.getElement7()) && this.getElement8().equals(t.getElement8()) && this.getElement9().equals(t.getElement9()) && this.getElement10().equals(t.getElement10()) && this.getElement11().equals(t.getElement11());
            }
            return super.equals(o);
        }


    }

    class Tuple13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        private T5 element5;
        public T5 getElement5(){
            return this.element5;
        }
        public void setElement5(T5 value){
            this.element5 = value;
        }


        private T6 element6;
        public T6 getElement6(){
            return this.element6;
        }
        public void setElement6(T6 value){
            this.element6 = value;
        }


        private T7 element7;
        public T7 getElement7(){
            return this.element7;
        }
        public void setElement7(T7 value){
            this.element7 = value;
        }


        private T8 element8;
        public T8 getElement8(){
            return this.element8;
        }
        public void setElement8(T8 value){
            this.element8 = value;
        }


        private T9 element9;
        public T9 getElement9(){
            return this.element9;
        }
        public void setElement9(T9 value){
            this.element9 = value;
        }


        private T10 element10;
        public T10 getElement10(){
            return this.element10;
        }
        public void setElement10(T10 value){
            this.element10 = value;
        }


        private T11 element11;
        public T11 getElement11(){
            return this.element11;
        }
        public void setElement11(T11 value){
            this.element11 = value;
        }


        private T12 element12;
        public T12 getElement12(){
            return this.element12;
        }
        public void setElement12(T12 value){
            this.element12 = value;
        }


        public Tuple13() {
        }

        public Tuple13(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8, T9 _9, T10 _10, T11 _11, T12 _12){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

            this.element5 = _5;

            this.element6 = _6;

            this.element7 = _7;

            this.element8 = _8;

            this.element9 = _9;

            this.element10 = _10;

            this.element11 = _11;

            this.element12 = _12;

        }

        @Override
        public int getLength() {
            return 13;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+ "," +
                    (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5 )+ "," +
                    (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6 )+ "," +
                    (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7 )+ "," +
                    (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8 )+ "," +
                    (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9 )+ "," +
                    (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10 )+ "," +
                    (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11 )+ "," +
                    (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple13){
                Tuple13 t = (Tuple13) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4()) && this.getElement5().equals(t.getElement5()) && this.getElement6().equals(t.getElement6()) && this.getElement7().equals(t.getElement7()) && this.getElement8().equals(t.getElement8()) && this.getElement9().equals(t.getElement9()) && this.getElement10().equals(t.getElement10()) && this.getElement11().equals(t.getElement11()) && this.getElement12().equals(t.getElement12());
            }
            return super.equals(o);
        }


    }

    class Tuple14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        private T5 element5;
        public T5 getElement5(){
            return this.element5;
        }
        public void setElement5(T5 value){
            this.element5 = value;
        }


        private T6 element6;
        public T6 getElement6(){
            return this.element6;
        }
        public void setElement6(T6 value){
            this.element6 = value;
        }


        private T7 element7;
        public T7 getElement7(){
            return this.element7;
        }
        public void setElement7(T7 value){
            this.element7 = value;
        }


        private T8 element8;
        public T8 getElement8(){
            return this.element8;
        }
        public void setElement8(T8 value){
            this.element8 = value;
        }


        private T9 element9;
        public T9 getElement9(){
            return this.element9;
        }
        public void setElement9(T9 value){
            this.element9 = value;
        }


        private T10 element10;
        public T10 getElement10(){
            return this.element10;
        }
        public void setElement10(T10 value){
            this.element10 = value;
        }


        private T11 element11;
        public T11 getElement11(){
            return this.element11;
        }
        public void setElement11(T11 value){
            this.element11 = value;
        }


        private T12 element12;
        public T12 getElement12(){
            return this.element12;
        }
        public void setElement12(T12 value){
            this.element12 = value;
        }


        private T13 element13;
        public T13 getElement13(){
            return this.element13;
        }
        public void setElement13(T13 value){
            this.element13 = value;
        }


        public Tuple14() {
        }

        public Tuple14(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8, T9 _9, T10 _10, T11 _11, T12 _12, T13 _13){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

            this.element5 = _5;

            this.element6 = _6;

            this.element7 = _7;

            this.element8 = _8;

            this.element9 = _9;

            this.element10 = _10;

            this.element11 = _11;

            this.element12 = _12;

            this.element13 = _13;

        }

        @Override
        public int getLength() {
            return 14;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+ "," +
                    (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5 )+ "," +
                    (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6 )+ "," +
                    (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7 )+ "," +
                    (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8 )+ "," +
                    (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9 )+ "," +
                    (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10 )+ "," +
                    (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11 )+ "," +
                    (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12 )+ "," +
                    (this.element13 instanceof String ? "\"" + this.element13 + "\"" : this.element13 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple14){
                Tuple14 t = (Tuple14) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4()) && this.getElement5().equals(t.getElement5()) && this.getElement6().equals(t.getElement6()) && this.getElement7().equals(t.getElement7()) && this.getElement8().equals(t.getElement8()) && this.getElement9().equals(t.getElement9()) && this.getElement10().equals(t.getElement10()) && this.getElement11().equals(t.getElement11()) && this.getElement12().equals(t.getElement12()) && this.getElement13().equals(t.getElement13());
            }
            return super.equals(o);
        }


    }

    class Tuple15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        private T5 element5;
        public T5 getElement5(){
            return this.element5;
        }
        public void setElement5(T5 value){
            this.element5 = value;
        }


        private T6 element6;
        public T6 getElement6(){
            return this.element6;
        }
        public void setElement6(T6 value){
            this.element6 = value;
        }


        private T7 element7;
        public T7 getElement7(){
            return this.element7;
        }
        public void setElement7(T7 value){
            this.element7 = value;
        }


        private T8 element8;
        public T8 getElement8(){
            return this.element8;
        }
        public void setElement8(T8 value){
            this.element8 = value;
        }


        private T9 element9;
        public T9 getElement9(){
            return this.element9;
        }
        public void setElement9(T9 value){
            this.element9 = value;
        }


        private T10 element10;
        public T10 getElement10(){
            return this.element10;
        }
        public void setElement10(T10 value){
            this.element10 = value;
        }


        private T11 element11;
        public T11 getElement11(){
            return this.element11;
        }
        public void setElement11(T11 value){
            this.element11 = value;
        }


        private T12 element12;
        public T12 getElement12(){
            return this.element12;
        }
        public void setElement12(T12 value){
            this.element12 = value;
        }


        private T13 element13;
        public T13 getElement13(){
            return this.element13;
        }
        public void setElement13(T13 value){
            this.element13 = value;
        }


        private T14 element14;
        public T14 getElement14(){
            return this.element14;
        }
        public void setElement14(T14 value){
            this.element14 = value;
        }


        public Tuple15() {
        }

        public Tuple15(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8, T9 _9, T10 _10, T11 _11, T12 _12, T13 _13, T14 _14){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

            this.element5 = _5;

            this.element6 = _6;

            this.element7 = _7;

            this.element8 = _8;

            this.element9 = _9;

            this.element10 = _10;

            this.element11 = _11;

            this.element12 = _12;

            this.element13 = _13;

            this.element14 = _14;

        }

        @Override
        public int getLength() {
            return 15;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+ "," +
                    (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5 )+ "," +
                    (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6 )+ "," +
                    (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7 )+ "," +
                    (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8 )+ "," +
                    (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9 )+ "," +
                    (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10 )+ "," +
                    (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11 )+ "," +
                    (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12 )+ "," +
                    (this.element13 instanceof String ? "\"" + this.element13 + "\"" : this.element13 )+ "," +
                    (this.element14 instanceof String ? "\"" + this.element14 + "\"" : this.element14 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple15){
                Tuple15 t = (Tuple15) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4()) && this.getElement5().equals(t.getElement5()) && this.getElement6().equals(t.getElement6()) && this.getElement7().equals(t.getElement7()) && this.getElement8().equals(t.getElement8()) && this.getElement9().equals(t.getElement9()) && this.getElement10().equals(t.getElement10()) && this.getElement11().equals(t.getElement11()) && this.getElement12().equals(t.getElement12()) && this.getElement13().equals(t.getElement13()) && this.getElement14().equals(t.getElement14());
            }
            return super.equals(o);
        }


    }

    class Tuple16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        private T5 element5;
        public T5 getElement5(){
            return this.element5;
        }
        public void setElement5(T5 value){
            this.element5 = value;
        }


        private T6 element6;
        public T6 getElement6(){
            return this.element6;
        }
        public void setElement6(T6 value){
            this.element6 = value;
        }


        private T7 element7;
        public T7 getElement7(){
            return this.element7;
        }
        public void setElement7(T7 value){
            this.element7 = value;
        }


        private T8 element8;
        public T8 getElement8(){
            return this.element8;
        }
        public void setElement8(T8 value){
            this.element8 = value;
        }


        private T9 element9;
        public T9 getElement9(){
            return this.element9;
        }
        public void setElement9(T9 value){
            this.element9 = value;
        }


        private T10 element10;
        public T10 getElement10(){
            return this.element10;
        }
        public void setElement10(T10 value){
            this.element10 = value;
        }


        private T11 element11;
        public T11 getElement11(){
            return this.element11;
        }
        public void setElement11(T11 value){
            this.element11 = value;
        }


        private T12 element12;
        public T12 getElement12(){
            return this.element12;
        }
        public void setElement12(T12 value){
            this.element12 = value;
        }


        private T13 element13;
        public T13 getElement13(){
            return this.element13;
        }
        public void setElement13(T13 value){
            this.element13 = value;
        }


        private T14 element14;
        public T14 getElement14(){
            return this.element14;
        }
        public void setElement14(T14 value){
            this.element14 = value;
        }


        private T15 element15;
        public T15 getElement15(){
            return this.element15;
        }
        public void setElement15(T15 value){
            this.element15 = value;
        }


        public Tuple16() {
        }

        public Tuple16(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8, T9 _9, T10 _10, T11 _11, T12 _12, T13 _13, T14 _14, T15 _15){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

            this.element5 = _5;

            this.element6 = _6;

            this.element7 = _7;

            this.element8 = _8;

            this.element9 = _9;

            this.element10 = _10;

            this.element11 = _11;

            this.element12 = _12;

            this.element13 = _13;

            this.element14 = _14;

            this.element15 = _15;

        }

        @Override
        public int getLength() {
            return 16;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+ "," +
                    (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5 )+ "," +
                    (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6 )+ "," +
                    (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7 )+ "," +
                    (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8 )+ "," +
                    (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9 )+ "," +
                    (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10 )+ "," +
                    (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11 )+ "," +
                    (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12 )+ "," +
                    (this.element13 instanceof String ? "\"" + this.element13 + "\"" : this.element13 )+ "," +
                    (this.element14 instanceof String ? "\"" + this.element14 + "\"" : this.element14 )+ "," +
                    (this.element15 instanceof String ? "\"" + this.element15 + "\"" : this.element15 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple16){
                Tuple16 t = (Tuple16) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4()) && this.getElement5().equals(t.getElement5()) && this.getElement6().equals(t.getElement6()) && this.getElement7().equals(t.getElement7()) && this.getElement8().equals(t.getElement8()) && this.getElement9().equals(t.getElement9()) && this.getElement10().equals(t.getElement10()) && this.getElement11().equals(t.getElement11()) && this.getElement12().equals(t.getElement12()) && this.getElement13().equals(t.getElement13()) && this.getElement14().equals(t.getElement14()) && this.getElement15().equals(t.getElement15());
            }
            return super.equals(o);
        }


    }

    class Tuple17<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        private T5 element5;
        public T5 getElement5(){
            return this.element5;
        }
        public void setElement5(T5 value){
            this.element5 = value;
        }


        private T6 element6;
        public T6 getElement6(){
            return this.element6;
        }
        public void setElement6(T6 value){
            this.element6 = value;
        }


        private T7 element7;
        public T7 getElement7(){
            return this.element7;
        }
        public void setElement7(T7 value){
            this.element7 = value;
        }


        private T8 element8;
        public T8 getElement8(){
            return this.element8;
        }
        public void setElement8(T8 value){
            this.element8 = value;
        }


        private T9 element9;
        public T9 getElement9(){
            return this.element9;
        }
        public void setElement9(T9 value){
            this.element9 = value;
        }


        private T10 element10;
        public T10 getElement10(){
            return this.element10;
        }
        public void setElement10(T10 value){
            this.element10 = value;
        }


        private T11 element11;
        public T11 getElement11(){
            return this.element11;
        }
        public void setElement11(T11 value){
            this.element11 = value;
        }


        private T12 element12;
        public T12 getElement12(){
            return this.element12;
        }
        public void setElement12(T12 value){
            this.element12 = value;
        }


        private T13 element13;
        public T13 getElement13(){
            return this.element13;
        }
        public void setElement13(T13 value){
            this.element13 = value;
        }


        private T14 element14;
        public T14 getElement14(){
            return this.element14;
        }
        public void setElement14(T14 value){
            this.element14 = value;
        }


        private T15 element15;
        public T15 getElement15(){
            return this.element15;
        }
        public void setElement15(T15 value){
            this.element15 = value;
        }


        private T16 element16;
        public T16 getElement16(){
            return this.element16;
        }
        public void setElement16(T16 value){
            this.element16 = value;
        }


        public Tuple17() {
        }

        public Tuple17(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8, T9 _9, T10 _10, T11 _11, T12 _12, T13 _13, T14 _14, T15 _15, T16 _16){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

            this.element5 = _5;

            this.element6 = _6;

            this.element7 = _7;

            this.element8 = _8;

            this.element9 = _9;

            this.element10 = _10;

            this.element11 = _11;

            this.element12 = _12;

            this.element13 = _13;

            this.element14 = _14;

            this.element15 = _15;

            this.element16 = _16;

        }

        @Override
        public int getLength() {
            return 17;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+ "," +
                    (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5 )+ "," +
                    (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6 )+ "," +
                    (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7 )+ "," +
                    (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8 )+ "," +
                    (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9 )+ "," +
                    (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10 )+ "," +
                    (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11 )+ "," +
                    (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12 )+ "," +
                    (this.element13 instanceof String ? "\"" + this.element13 + "\"" : this.element13 )+ "," +
                    (this.element14 instanceof String ? "\"" + this.element14 + "\"" : this.element14 )+ "," +
                    (this.element15 instanceof String ? "\"" + this.element15 + "\"" : this.element15 )+ "," +
                    (this.element16 instanceof String ? "\"" + this.element16 + "\"" : this.element16 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple17){
                Tuple17 t = (Tuple17) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4()) && this.getElement5().equals(t.getElement5()) && this.getElement6().equals(t.getElement6()) && this.getElement7().equals(t.getElement7()) && this.getElement8().equals(t.getElement8()) && this.getElement9().equals(t.getElement9()) && this.getElement10().equals(t.getElement10()) && this.getElement11().equals(t.getElement11()) && this.getElement12().equals(t.getElement12()) && this.getElement13().equals(t.getElement13()) && this.getElement14().equals(t.getElement14()) && this.getElement15().equals(t.getElement15()) && this.getElement16().equals(t.getElement16());
            }
            return super.equals(o);
        }


    }

    class Tuple18<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        private T5 element5;
        public T5 getElement5(){
            return this.element5;
        }
        public void setElement5(T5 value){
            this.element5 = value;
        }


        private T6 element6;
        public T6 getElement6(){
            return this.element6;
        }
        public void setElement6(T6 value){
            this.element6 = value;
        }


        private T7 element7;
        public T7 getElement7(){
            return this.element7;
        }
        public void setElement7(T7 value){
            this.element7 = value;
        }


        private T8 element8;
        public T8 getElement8(){
            return this.element8;
        }
        public void setElement8(T8 value){
            this.element8 = value;
        }


        private T9 element9;
        public T9 getElement9(){
            return this.element9;
        }
        public void setElement9(T9 value){
            this.element9 = value;
        }


        private T10 element10;
        public T10 getElement10(){
            return this.element10;
        }
        public void setElement10(T10 value){
            this.element10 = value;
        }


        private T11 element11;
        public T11 getElement11(){
            return this.element11;
        }
        public void setElement11(T11 value){
            this.element11 = value;
        }


        private T12 element12;
        public T12 getElement12(){
            return this.element12;
        }
        public void setElement12(T12 value){
            this.element12 = value;
        }


        private T13 element13;
        public T13 getElement13(){
            return this.element13;
        }
        public void setElement13(T13 value){
            this.element13 = value;
        }


        private T14 element14;
        public T14 getElement14(){
            return this.element14;
        }
        public void setElement14(T14 value){
            this.element14 = value;
        }


        private T15 element15;
        public T15 getElement15(){
            return this.element15;
        }
        public void setElement15(T15 value){
            this.element15 = value;
        }


        private T16 element16;
        public T16 getElement16(){
            return this.element16;
        }
        public void setElement16(T16 value){
            this.element16 = value;
        }


        private T17 element17;
        public T17 getElement17(){
            return this.element17;
        }
        public void setElement17(T17 value){
            this.element17 = value;
        }


        public Tuple18() {
        }

        public Tuple18(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8, T9 _9, T10 _10, T11 _11, T12 _12, T13 _13, T14 _14, T15 _15, T16 _16, T17 _17){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

            this.element5 = _5;

            this.element6 = _6;

            this.element7 = _7;

            this.element8 = _8;

            this.element9 = _9;

            this.element10 = _10;

            this.element11 = _11;

            this.element12 = _12;

            this.element13 = _13;

            this.element14 = _14;

            this.element15 = _15;

            this.element16 = _16;

            this.element17 = _17;

        }

        @Override
        public int getLength() {
            return 18;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+ "," +
                    (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5 )+ "," +
                    (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6 )+ "," +
                    (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7 )+ "," +
                    (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8 )+ "," +
                    (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9 )+ "," +
                    (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10 )+ "," +
                    (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11 )+ "," +
                    (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12 )+ "," +
                    (this.element13 instanceof String ? "\"" + this.element13 + "\"" : this.element13 )+ "," +
                    (this.element14 instanceof String ? "\"" + this.element14 + "\"" : this.element14 )+ "," +
                    (this.element15 instanceof String ? "\"" + this.element15 + "\"" : this.element15 )+ "," +
                    (this.element16 instanceof String ? "\"" + this.element16 + "\"" : this.element16 )+ "," +
                    (this.element17 instanceof String ? "\"" + this.element17 + "\"" : this.element17 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple18){
                Tuple18 t = (Tuple18) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4()) && this.getElement5().equals(t.getElement5()) && this.getElement6().equals(t.getElement6()) && this.getElement7().equals(t.getElement7()) && this.getElement8().equals(t.getElement8()) && this.getElement9().equals(t.getElement9()) && this.getElement10().equals(t.getElement10()) && this.getElement11().equals(t.getElement11()) && this.getElement12().equals(t.getElement12()) && this.getElement13().equals(t.getElement13()) && this.getElement14().equals(t.getElement14()) && this.getElement15().equals(t.getElement15()) && this.getElement16().equals(t.getElement16()) && this.getElement17().equals(t.getElement17());
            }
            return super.equals(o);
        }


    }

    class Tuple19<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        private T5 element5;
        public T5 getElement5(){
            return this.element5;
        }
        public void setElement5(T5 value){
            this.element5 = value;
        }


        private T6 element6;
        public T6 getElement6(){
            return this.element6;
        }
        public void setElement6(T6 value){
            this.element6 = value;
        }


        private T7 element7;
        public T7 getElement7(){
            return this.element7;
        }
        public void setElement7(T7 value){
            this.element7 = value;
        }


        private T8 element8;
        public T8 getElement8(){
            return this.element8;
        }
        public void setElement8(T8 value){
            this.element8 = value;
        }


        private T9 element9;
        public T9 getElement9(){
            return this.element9;
        }
        public void setElement9(T9 value){
            this.element9 = value;
        }


        private T10 element10;
        public T10 getElement10(){
            return this.element10;
        }
        public void setElement10(T10 value){
            this.element10 = value;
        }


        private T11 element11;
        public T11 getElement11(){
            return this.element11;
        }
        public void setElement11(T11 value){
            this.element11 = value;
        }


        private T12 element12;
        public T12 getElement12(){
            return this.element12;
        }
        public void setElement12(T12 value){
            this.element12 = value;
        }


        private T13 element13;
        public T13 getElement13(){
            return this.element13;
        }
        public void setElement13(T13 value){
            this.element13 = value;
        }


        private T14 element14;
        public T14 getElement14(){
            return this.element14;
        }
        public void setElement14(T14 value){
            this.element14 = value;
        }


        private T15 element15;
        public T15 getElement15(){
            return this.element15;
        }
        public void setElement15(T15 value){
            this.element15 = value;
        }


        private T16 element16;
        public T16 getElement16(){
            return this.element16;
        }
        public void setElement16(T16 value){
            this.element16 = value;
        }


        private T17 element17;
        public T17 getElement17(){
            return this.element17;
        }
        public void setElement17(T17 value){
            this.element17 = value;
        }


        private T18 element18;
        public T18 getElement18(){
            return this.element18;
        }
        public void setElement18(T18 value){
            this.element18 = value;
        }


        public Tuple19() {
        }

        public Tuple19(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8, T9 _9, T10 _10, T11 _11, T12 _12, T13 _13, T14 _14, T15 _15, T16 _16, T17 _17, T18 _18){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

            this.element5 = _5;

            this.element6 = _6;

            this.element7 = _7;

            this.element8 = _8;

            this.element9 = _9;

            this.element10 = _10;

            this.element11 = _11;

            this.element12 = _12;

            this.element13 = _13;

            this.element14 = _14;

            this.element15 = _15;

            this.element16 = _16;

            this.element17 = _17;

            this.element18 = _18;

        }

        @Override
        public int getLength() {
            return 19;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+ "," +
                    (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5 )+ "," +
                    (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6 )+ "," +
                    (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7 )+ "," +
                    (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8 )+ "," +
                    (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9 )+ "," +
                    (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10 )+ "," +
                    (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11 )+ "," +
                    (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12 )+ "," +
                    (this.element13 instanceof String ? "\"" + this.element13 + "\"" : this.element13 )+ "," +
                    (this.element14 instanceof String ? "\"" + this.element14 + "\"" : this.element14 )+ "," +
                    (this.element15 instanceof String ? "\"" + this.element15 + "\"" : this.element15 )+ "," +
                    (this.element16 instanceof String ? "\"" + this.element16 + "\"" : this.element16 )+ "," +
                    (this.element17 instanceof String ? "\"" + this.element17 + "\"" : this.element17 )+ "," +
                    (this.element18 instanceof String ? "\"" + this.element18 + "\"" : this.element18 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple19){
                Tuple19 t = (Tuple19) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4()) && this.getElement5().equals(t.getElement5()) && this.getElement6().equals(t.getElement6()) && this.getElement7().equals(t.getElement7()) && this.getElement8().equals(t.getElement8()) && this.getElement9().equals(t.getElement9()) && this.getElement10().equals(t.getElement10()) && this.getElement11().equals(t.getElement11()) && this.getElement12().equals(t.getElement12()) && this.getElement13().equals(t.getElement13()) && this.getElement14().equals(t.getElement14()) && this.getElement15().equals(t.getElement15()) && this.getElement16().equals(t.getElement16()) && this.getElement17().equals(t.getElement17()) && this.getElement18().equals(t.getElement18());
            }
            return super.equals(o);
        }


    }

    class Tuple20<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> implements Tuple{


        private T0 element0;
        public T0 getElement0(){
            return this.element0;
        }
        public void setElement0(T0 value){
            this.element0 = value;
        }


        private T1 element1;
        public T1 getElement1(){
            return this.element1;
        }
        public void setElement1(T1 value){
            this.element1 = value;
        }


        private T2 element2;
        public T2 getElement2(){
            return this.element2;
        }
        public void setElement2(T2 value){
            this.element2 = value;
        }


        private T3 element3;
        public T3 getElement3(){
            return this.element3;
        }
        public void setElement3(T3 value){
            this.element3 = value;
        }


        private T4 element4;
        public T4 getElement4(){
            return this.element4;
        }
        public void setElement4(T4 value){
            this.element4 = value;
        }


        private T5 element5;
        public T5 getElement5(){
            return this.element5;
        }
        public void setElement5(T5 value){
            this.element5 = value;
        }


        private T6 element6;
        public T6 getElement6(){
            return this.element6;
        }
        public void setElement6(T6 value){
            this.element6 = value;
        }


        private T7 element7;
        public T7 getElement7(){
            return this.element7;
        }
        public void setElement7(T7 value){
            this.element7 = value;
        }


        private T8 element8;
        public T8 getElement8(){
            return this.element8;
        }
        public void setElement8(T8 value){
            this.element8 = value;
        }


        private T9 element9;
        public T9 getElement9(){
            return this.element9;
        }
        public void setElement9(T9 value){
            this.element9 = value;
        }


        private T10 element10;
        public T10 getElement10(){
            return this.element10;
        }
        public void setElement10(T10 value){
            this.element10 = value;
        }


        private T11 element11;
        public T11 getElement11(){
            return this.element11;
        }
        public void setElement11(T11 value){
            this.element11 = value;
        }


        private T12 element12;
        public T12 getElement12(){
            return this.element12;
        }
        public void setElement12(T12 value){
            this.element12 = value;
        }


        private T13 element13;
        public T13 getElement13(){
            return this.element13;
        }
        public void setElement13(T13 value){
            this.element13 = value;
        }


        private T14 element14;
        public T14 getElement14(){
            return this.element14;
        }
        public void setElement14(T14 value){
            this.element14 = value;
        }


        private T15 element15;
        public T15 getElement15(){
            return this.element15;
        }
        public void setElement15(T15 value){
            this.element15 = value;
        }


        private T16 element16;
        public T16 getElement16(){
            return this.element16;
        }
        public void setElement16(T16 value){
            this.element16 = value;
        }


        private T17 element17;
        public T17 getElement17(){
            return this.element17;
        }
        public void setElement17(T17 value){
            this.element17 = value;
        }


        private T18 element18;
        public T18 getElement18(){
            return this.element18;
        }
        public void setElement18(T18 value){
            this.element18 = value;
        }


        private T19 element19;
        public T19 getElement19(){
            return this.element19;
        }
        public void setElement19(T19 value){
            this.element19 = value;
        }


        public Tuple20() {
        }

        public Tuple20(T0 _0, T1 _1, T2 _2, T3 _3, T4 _4, T5 _5, T6 _6, T7 _7, T8 _8, T9 _9, T10 _10, T11 _11, T12 _12, T13 _13, T14 _14, T15 _15, T16 _16, T17 _17, T18 _18, T19 _19){

            this.element0 = _0;

            this.element1 = _1;

            this.element2 = _2;

            this.element3 = _3;

            this.element4 = _4;

            this.element5 = _5;

            this.element6 = _6;

            this.element7 = _7;

            this.element8 = _8;

            this.element9 = _9;

            this.element10 = _10;

            this.element11 = _11;

            this.element12 = _12;

            this.element13 = _13;

            this.element14 = _14;

            this.element15 = _15;

            this.element16 = _16;

            this.element17 = _17;

            this.element18 = _18;

            this.element19 = _19;

        }

        @Override
        public int getLength() {
            return 20;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0 )+ "," +
                    (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1 )+ "," +
                    (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2 )+ "," +
                    (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3 )+ "," +
                    (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4 )+ "," +
                    (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5 )+ "," +
                    (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6 )+ "," +
                    (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7 )+ "," +
                    (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8 )+ "," +
                    (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9 )+ "," +
                    (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10 )+ "," +
                    (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11 )+ "," +
                    (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12 )+ "," +
                    (this.element13 instanceof String ? "\"" + this.element13 + "\"" : this.element13 )+ "," +
                    (this.element14 instanceof String ? "\"" + this.element14 + "\"" : this.element14 )+ "," +
                    (this.element15 instanceof String ? "\"" + this.element15 + "\"" : this.element15 )+ "," +
                    (this.element16 instanceof String ? "\"" + this.element16 + "\"" : this.element16 )+ "," +
                    (this.element17 instanceof String ? "\"" + this.element17 + "\"" : this.element17 )+ "," +
                    (this.element18 instanceof String ? "\"" + this.element18 + "\"" : this.element18 )+ "," +
                    (this.element19 instanceof String ? "\"" + this.element19 + "\"" : this.element19 )+  ")";
        }

        @Override
        public boolean equals(Object o){
            if(o instanceof Tuple20){
                Tuple20 t = (Tuple20) o;
                return this.getElement0().equals(t.getElement0()) && this.getElement1().equals(t.getElement1()) && this.getElement2().equals(t.getElement2()) && this.getElement3().equals(t.getElement3()) && this.getElement4().equals(t.getElement4()) && this.getElement5().equals(t.getElement5()) && this.getElement6().equals(t.getElement6()) && this.getElement7().equals(t.getElement7()) && this.getElement8().equals(t.getElement8()) && this.getElement9().equals(t.getElement9()) && this.getElement10().equals(t.getElement10()) && this.getElement11().equals(t.getElement11()) && this.getElement12().equals(t.getElement12()) && this.getElement13().equals(t.getElement13()) && this.getElement14().equals(t.getElement14()) && this.getElement15().equals(t.getElement15()) && this.getElement16().equals(t.getElement16()) && this.getElement17().equals(t.getElement17()) && this.getElement18().equals(t.getElement18()) && this.getElement19().equals(t.getElement19());
            }
            return super.equals(o);
        }


    }






}
