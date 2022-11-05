package jadescript.lang;

import jade.content.AgentAction;

@SuppressWarnings({"rawtypes", "unused"})
public interface Tuple extends AgentAction {
    int getLength();

    class Tuple0 implements Tuple {

        public Tuple0() {

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
        public boolean equals(Object o) {
            if (o instanceof Tuple0) {
                Tuple0 t = (Tuple0) o;
                return true;
            }
            return super.equals(o);
        }

        public <X> Tuple1<X> add(X x) {
            return new Tuple1<>(x);
        }
    }

    class Tuple1<T0> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        public Tuple1() {
        }

        public Tuple1(T0 _0) {

            this.element0 = _0;

        }

        @Override
        public int getLength() {
            return 1;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Tuple1<?> tuple1 = (Tuple1<?>) o;

            return getElement0() != null ? getElement0().equals(tuple1.getElement0()) : tuple1.getElement0() == null;
        }

        @Override
        public int hashCode() {
            return getElement0() != null ? getElement0().hashCode() : 0;
        }

        public <X> Tuple2<T0, X> add(X x) {
            return new Tuple2<>(getElement0(), x);
        }
    }

    class Tuple2<T0, T1> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        public Tuple2() {
        }


        public Tuple2(
                T0 _0,
                T1 _1
        ) {
            this.element0 = _0;
            this.element1 = _1;
        }

        @Override
        public int getLength() {
            return 2;
        }

        @Override
        public String toString() {
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple2<?, ?> t = (Tuple2<?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            return getElement1().equals(t.getElement1());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            return result;
        }

        public <X> Tuple3<T0, T1, X> add(X x) {
            return new Tuple3<>(getElement0(), getElement1(), x);
        }

    }

    class Tuple3<T0, T1, T2> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        public Tuple3() {
        }


        public Tuple3(
                T0 _0,
                T1 _1,
                T2 _2
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple3<?, ?, ?> t = (Tuple3<?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            return getElement2().equals(t.getElement2());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            return result;
        }

        public <X> Tuple4<T0, T1, T2, X> add(X x) {
            return new Tuple4<>(getElement0(), getElement1(), getElement2(), x);
        }

    }

    class Tuple4<T0, T1, T2, T3> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        public Tuple4() {
        }


        public Tuple4(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple4<?, ?, ?, ?> t = (Tuple4<?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            return getElement3().equals(t.getElement3());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            return result;
        }

        public <X> Tuple5<T0, T1, T2, T3, X> add(X x) {
            return new Tuple5<>(getElement0(), getElement1(), getElement2(), getElement3(), x);
        }

    }

    class Tuple5<T0, T1, T2, T3, T4> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        public Tuple5() {
        }


        public Tuple5(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple5<?, ?, ?, ?, ?> t = (Tuple5<?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            return getElement4().equals(t.getElement4());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            return result;
        }

        public <X> Tuple6<T0, T1, T2, T3, T4, X> add(X x) {
            return new Tuple6<>(getElement0(), getElement1(), getElement2(), getElement3(), getElement4(), x);
        }

    }

    class Tuple6<T0, T1, T2, T3, T4, T5> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        private T5 element5;

        public T5 getElement5() {
            return this.element5;
        }

        public void setElement5(T5 value) {
            this.element5 = value;
        }


        public Tuple6() {
        }


        public Tuple6(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4,
                T5 _5
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ","
                    + (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple6<?, ?, ?, ?, ?, ?> t = (Tuple6<?, ?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            if (!getElement4().equals(t.getElement4())) return false;
            return getElement5().equals(t.getElement5());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            result = 31 * result + getElement5().hashCode();
            return result;
        }

        public <X> Tuple7<T0, T1, T2, T3, T4, T5, X> add(X x) {
            return new Tuple7<>(getElement0(), getElement1(), getElement2(), getElement3(), getElement4(), getElement5(), x);
        }

    }

    class Tuple7<T0, T1, T2, T3, T4, T5, T6> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        private T5 element5;

        public T5 getElement5() {
            return this.element5;
        }

        public void setElement5(T5 value) {
            this.element5 = value;
        }


        private T6 element6;

        public T6 getElement6() {
            return this.element6;
        }

        public void setElement6(T6 value) {
            this.element6 = value;
        }


        public Tuple7() {
        }


        public Tuple7(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4,
                T5 _5,
                T6 _6
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ","
                    + (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5) + ","
                    + (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple7<?, ?, ?, ?, ?, ?, ?> t = (Tuple7<?, ?, ?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            if (!getElement4().equals(t.getElement4())) return false;
            if (!getElement5().equals(t.getElement5())) return false;
            return getElement6().equals(t.getElement6());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            result = 31 * result + getElement5().hashCode();
            result = 31 * result + getElement6().hashCode();
            return result;
        }

        public <X> Tuple8<T0, T1, T2, T3, T4, T5, T6, X> add(X x) {
            return new Tuple8<>(getElement0(), getElement1(), getElement2(), getElement3(), getElement4(), getElement5(), getElement6(), x);
        }

    }

    class Tuple8<T0, T1, T2, T3, T4, T5, T6, T7> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        private T5 element5;

        public T5 getElement5() {
            return this.element5;
        }

        public void setElement5(T5 value) {
            this.element5 = value;
        }


        private T6 element6;

        public T6 getElement6() {
            return this.element6;
        }

        public void setElement6(T6 value) {
            this.element6 = value;
        }


        private T7 element7;

        public T7 getElement7() {
            return this.element7;
        }

        public void setElement7(T7 value) {
            this.element7 = value;
        }


        public Tuple8() {
        }


        public Tuple8(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4,
                T5 _5,
                T6 _6,
                T7 _7
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ","
                    + (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5) + ","
                    + (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6) + ","
                    + (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple8<?, ?, ?, ?, ?, ?, ?, ?> t = (Tuple8<?, ?, ?, ?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            if (!getElement4().equals(t.getElement4())) return false;
            if (!getElement5().equals(t.getElement5())) return false;
            if (!getElement6().equals(t.getElement6())) return false;
            return getElement7().equals(t.getElement7());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            result = 31 * result + getElement5().hashCode();
            result = 31 * result + getElement6().hashCode();
            result = 31 * result + getElement7().hashCode();
            return result;
        }

        public <X> Tuple9<T0, T1, T2, T3, T4, T5, T6, T7, X> add(X x) {
            return new Tuple9<>(getElement0(), getElement1(), getElement2(), getElement3(), getElement4(), getElement5(), getElement6(), getElement7(), x);
        }

    }

    class Tuple9<T0, T1, T2, T3, T4, T5, T6, T7, T8> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        private T5 element5;

        public T5 getElement5() {
            return this.element5;
        }

        public void setElement5(T5 value) {
            this.element5 = value;
        }


        private T6 element6;

        public T6 getElement6() {
            return this.element6;
        }

        public void setElement6(T6 value) {
            this.element6 = value;
        }


        private T7 element7;

        public T7 getElement7() {
            return this.element7;
        }

        public void setElement7(T7 value) {
            this.element7 = value;
        }


        private T8 element8;

        public T8 getElement8() {
            return this.element8;
        }

        public void setElement8(T8 value) {
            this.element8 = value;
        }


        public Tuple9() {
        }


        public Tuple9(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4,
                T5 _5,
                T6 _6,
                T7 _7,
                T8 _8
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ","
                    + (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5) + ","
                    + (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6) + ","
                    + (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7) + ","
                    + (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?> t = (Tuple9<?, ?, ?, ?, ?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            if (!getElement4().equals(t.getElement4())) return false;
            if (!getElement5().equals(t.getElement5())) return false;
            if (!getElement6().equals(t.getElement6())) return false;
            if (!getElement7().equals(t.getElement7())) return false;
            return getElement8().equals(t.getElement8());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            result = 31 * result + getElement5().hashCode();
            result = 31 * result + getElement6().hashCode();
            result = 31 * result + getElement7().hashCode();
            result = 31 * result + getElement8().hashCode();
            return result;
        }

        public <X> Tuple10<T0, T1, T2, T3, T4, T5, T6, T7, T8, X> add(X x) {
            return new Tuple10<>(getElement0(), getElement1(), getElement2(), getElement3(), getElement4(), getElement5(), getElement6(), getElement7(), getElement8(), x);
        }

    }

    class Tuple10<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        private T5 element5;

        public T5 getElement5() {
            return this.element5;
        }

        public void setElement5(T5 value) {
            this.element5 = value;
        }


        private T6 element6;

        public T6 getElement6() {
            return this.element6;
        }

        public void setElement6(T6 value) {
            this.element6 = value;
        }


        private T7 element7;

        public T7 getElement7() {
            return this.element7;
        }

        public void setElement7(T7 value) {
            this.element7 = value;
        }


        private T8 element8;

        public T8 getElement8() {
            return this.element8;
        }

        public void setElement8(T8 value) {
            this.element8 = value;
        }


        private T9 element9;

        public T9 getElement9() {
            return this.element9;
        }

        public void setElement9(T9 value) {
            this.element9 = value;
        }


        public Tuple10() {
        }


        public Tuple10(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4,
                T5 _5,
                T6 _6,
                T7 _7,
                T8 _8,
                T9 _9
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ","
                    + (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5) + ","
                    + (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6) + ","
                    + (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7) + ","
                    + (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8) + ","
                    + (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple10<?, ?, ?, ?, ?, ?, ?, ?, ?, ?> t = (Tuple10<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            if (!getElement4().equals(t.getElement4())) return false;
            if (!getElement5().equals(t.getElement5())) return false;
            if (!getElement6().equals(t.getElement6())) return false;
            if (!getElement7().equals(t.getElement7())) return false;
            if (!getElement8().equals(t.getElement8())) return false;
            return getElement9().equals(t.getElement9());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            result = 31 * result + getElement5().hashCode();
            result = 31 * result + getElement6().hashCode();
            result = 31 * result + getElement7().hashCode();
            result = 31 * result + getElement8().hashCode();
            result = 31 * result + getElement9().hashCode();
            return result;
        }

        public <X> Tuple11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, X> add(X x) {
            return new Tuple11<>(getElement0(), getElement1(), getElement2(), getElement3(), getElement4(), getElement5(), getElement6(), getElement7(), getElement8(), getElement9(), x);
        }

    }

    class Tuple11<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        private T5 element5;

        public T5 getElement5() {
            return this.element5;
        }

        public void setElement5(T5 value) {
            this.element5 = value;
        }


        private T6 element6;

        public T6 getElement6() {
            return this.element6;
        }

        public void setElement6(T6 value) {
            this.element6 = value;
        }


        private T7 element7;

        public T7 getElement7() {
            return this.element7;
        }

        public void setElement7(T7 value) {
            this.element7 = value;
        }


        private T8 element8;

        public T8 getElement8() {
            return this.element8;
        }

        public void setElement8(T8 value) {
            this.element8 = value;
        }


        private T9 element9;

        public T9 getElement9() {
            return this.element9;
        }

        public void setElement9(T9 value) {
            this.element9 = value;
        }


        private T10 element10;

        public T10 getElement10() {
            return this.element10;
        }

        public void setElement10(T10 value) {
            this.element10 = value;
        }


        public Tuple11() {
        }


        public Tuple11(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4,
                T5 _5,
                T6 _6,
                T7 _7,
                T8 _8,
                T9 _9,
                T10 _10
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ","
                    + (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5) + ","
                    + (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6) + ","
                    + (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7) + ","
                    + (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8) + ","
                    + (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9) + ","
                    + (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple11<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> t = (Tuple11<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            if (!getElement4().equals(t.getElement4())) return false;
            if (!getElement5().equals(t.getElement5())) return false;
            if (!getElement6().equals(t.getElement6())) return false;
            if (!getElement7().equals(t.getElement7())) return false;
            if (!getElement8().equals(t.getElement8())) return false;
            if (!getElement9().equals(t.getElement9())) return false;
            return getElement10().equals(t.getElement10());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            result = 31 * result + getElement5().hashCode();
            result = 31 * result + getElement6().hashCode();
            result = 31 * result + getElement7().hashCode();
            result = 31 * result + getElement8().hashCode();
            result = 31 * result + getElement9().hashCode();
            result = 31 * result + getElement10().hashCode();
            return result;
        }

        public <X> Tuple12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, X> add(X x) {
            return new Tuple12<>(getElement0(), getElement1(), getElement2(), getElement3(), getElement4(), getElement5(), getElement6(), getElement7(), getElement8(), getElement9(), getElement10(), x);
        }

    }

    class Tuple12<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        private T5 element5;

        public T5 getElement5() {
            return this.element5;
        }

        public void setElement5(T5 value) {
            this.element5 = value;
        }


        private T6 element6;

        public T6 getElement6() {
            return this.element6;
        }

        public void setElement6(T6 value) {
            this.element6 = value;
        }


        private T7 element7;

        public T7 getElement7() {
            return this.element7;
        }

        public void setElement7(T7 value) {
            this.element7 = value;
        }


        private T8 element8;

        public T8 getElement8() {
            return this.element8;
        }

        public void setElement8(T8 value) {
            this.element8 = value;
        }


        private T9 element9;

        public T9 getElement9() {
            return this.element9;
        }

        public void setElement9(T9 value) {
            this.element9 = value;
        }


        private T10 element10;

        public T10 getElement10() {
            return this.element10;
        }

        public void setElement10(T10 value) {
            this.element10 = value;
        }


        private T11 element11;

        public T11 getElement11() {
            return this.element11;
        }

        public void setElement11(T11 value) {
            this.element11 = value;
        }


        public Tuple12() {
        }


        public Tuple12(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4,
                T5 _5,
                T6 _6,
                T7 _7,
                T8 _8,
                T9 _9,
                T10 _10,
                T11 _11
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ","
                    + (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5) + ","
                    + (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6) + ","
                    + (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7) + ","
                    + (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8) + ","
                    + (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9) + ","
                    + (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10) + ","
                    + (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple12<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> t = (Tuple12<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            if (!getElement4().equals(t.getElement4())) return false;
            if (!getElement5().equals(t.getElement5())) return false;
            if (!getElement6().equals(t.getElement6())) return false;
            if (!getElement7().equals(t.getElement7())) return false;
            if (!getElement8().equals(t.getElement8())) return false;
            if (!getElement9().equals(t.getElement9())) return false;
            if (!getElement10().equals(t.getElement10())) return false;
            return getElement11().equals(t.getElement11());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            result = 31 * result + getElement5().hashCode();
            result = 31 * result + getElement6().hashCode();
            result = 31 * result + getElement7().hashCode();
            result = 31 * result + getElement8().hashCode();
            result = 31 * result + getElement9().hashCode();
            result = 31 * result + getElement10().hashCode();
            result = 31 * result + getElement11().hashCode();
            return result;
        }

        public <X> Tuple13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, X> add(X x) {
            return new Tuple13<>(getElement0(), getElement1(), getElement2(), getElement3(), getElement4(), getElement5(), getElement6(), getElement7(), getElement8(), getElement9(), getElement10(), getElement11(), x);
        }

    }

    class Tuple13<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        private T5 element5;

        public T5 getElement5() {
            return this.element5;
        }

        public void setElement5(T5 value) {
            this.element5 = value;
        }


        private T6 element6;

        public T6 getElement6() {
            return this.element6;
        }

        public void setElement6(T6 value) {
            this.element6 = value;
        }


        private T7 element7;

        public T7 getElement7() {
            return this.element7;
        }

        public void setElement7(T7 value) {
            this.element7 = value;
        }


        private T8 element8;

        public T8 getElement8() {
            return this.element8;
        }

        public void setElement8(T8 value) {
            this.element8 = value;
        }


        private T9 element9;

        public T9 getElement9() {
            return this.element9;
        }

        public void setElement9(T9 value) {
            this.element9 = value;
        }


        private T10 element10;

        public T10 getElement10() {
            return this.element10;
        }

        public void setElement10(T10 value) {
            this.element10 = value;
        }


        private T11 element11;

        public T11 getElement11() {
            return this.element11;
        }

        public void setElement11(T11 value) {
            this.element11 = value;
        }


        private T12 element12;

        public T12 getElement12() {
            return this.element12;
        }

        public void setElement12(T12 value) {
            this.element12 = value;
        }


        public Tuple13() {
        }


        public Tuple13(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4,
                T5 _5,
                T6 _6,
                T7 _7,
                T8 _8,
                T9 _9,
                T10 _10,
                T11 _11,
                T12 _12
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ","
                    + (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5) + ","
                    + (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6) + ","
                    + (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7) + ","
                    + (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8) + ","
                    + (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9) + ","
                    + (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10) + ","
                    + (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11) + ","
                    + (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple13<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> t = (Tuple13<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            if (!getElement4().equals(t.getElement4())) return false;
            if (!getElement5().equals(t.getElement5())) return false;
            if (!getElement6().equals(t.getElement6())) return false;
            if (!getElement7().equals(t.getElement7())) return false;
            if (!getElement8().equals(t.getElement8())) return false;
            if (!getElement9().equals(t.getElement9())) return false;
            if (!getElement10().equals(t.getElement10())) return false;
            if (!getElement11().equals(t.getElement11())) return false;
            return getElement12().equals(t.getElement12());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            result = 31 * result + getElement5().hashCode();
            result = 31 * result + getElement6().hashCode();
            result = 31 * result + getElement7().hashCode();
            result = 31 * result + getElement8().hashCode();
            result = 31 * result + getElement9().hashCode();
            result = 31 * result + getElement10().hashCode();
            result = 31 * result + getElement11().hashCode();
            result = 31 * result + getElement12().hashCode();
            return result;
        }

        public <X> Tuple14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, X> add(X x) {
            return new Tuple14<>(getElement0(), getElement1(), getElement2(), getElement3(), getElement4(), getElement5(), getElement6(), getElement7(), getElement8(), getElement9(), getElement10(), getElement11(), getElement12(), x);
        }

    }

    class Tuple14<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        private T5 element5;

        public T5 getElement5() {
            return this.element5;
        }

        public void setElement5(T5 value) {
            this.element5 = value;
        }


        private T6 element6;

        public T6 getElement6() {
            return this.element6;
        }

        public void setElement6(T6 value) {
            this.element6 = value;
        }


        private T7 element7;

        public T7 getElement7() {
            return this.element7;
        }

        public void setElement7(T7 value) {
            this.element7 = value;
        }


        private T8 element8;

        public T8 getElement8() {
            return this.element8;
        }

        public void setElement8(T8 value) {
            this.element8 = value;
        }


        private T9 element9;

        public T9 getElement9() {
            return this.element9;
        }

        public void setElement9(T9 value) {
            this.element9 = value;
        }


        private T10 element10;

        public T10 getElement10() {
            return this.element10;
        }

        public void setElement10(T10 value) {
            this.element10 = value;
        }


        private T11 element11;

        public T11 getElement11() {
            return this.element11;
        }

        public void setElement11(T11 value) {
            this.element11 = value;
        }


        private T12 element12;

        public T12 getElement12() {
            return this.element12;
        }

        public void setElement12(T12 value) {
            this.element12 = value;
        }


        private T13 element13;

        public T13 getElement13() {
            return this.element13;
        }

        public void setElement13(T13 value) {
            this.element13 = value;
        }


        public Tuple14() {
        }


        public Tuple14(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4,
                T5 _5,
                T6 _6,
                T7 _7,
                T8 _8,
                T9 _9,
                T10 _10,
                T11 _11,
                T12 _12,
                T13 _13
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ","
                    + (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5) + ","
                    + (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6) + ","
                    + (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7) + ","
                    + (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8) + ","
                    + (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9) + ","
                    + (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10) + ","
                    + (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11) + ","
                    + (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12) + ","
                    + (this.element13 instanceof String ? "\"" + this.element13 + "\"" : this.element13) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple14<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> t = (Tuple14<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            if (!getElement4().equals(t.getElement4())) return false;
            if (!getElement5().equals(t.getElement5())) return false;
            if (!getElement6().equals(t.getElement6())) return false;
            if (!getElement7().equals(t.getElement7())) return false;
            if (!getElement8().equals(t.getElement8())) return false;
            if (!getElement9().equals(t.getElement9())) return false;
            if (!getElement10().equals(t.getElement10())) return false;
            if (!getElement11().equals(t.getElement11())) return false;
            if (!getElement12().equals(t.getElement12())) return false;
            return getElement13().equals(t.getElement13());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            result = 31 * result + getElement5().hashCode();
            result = 31 * result + getElement6().hashCode();
            result = 31 * result + getElement7().hashCode();
            result = 31 * result + getElement8().hashCode();
            result = 31 * result + getElement9().hashCode();
            result = 31 * result + getElement10().hashCode();
            result = 31 * result + getElement11().hashCode();
            result = 31 * result + getElement12().hashCode();
            result = 31 * result + getElement13().hashCode();
            return result;
        }

        public <X> Tuple15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, X> add(X x) {
            return new Tuple15<>(getElement0(), getElement1(), getElement2(), getElement3(), getElement4(), getElement5(), getElement6(), getElement7(), getElement8(), getElement9(), getElement10(), getElement11(), getElement12(), getElement13(), x);
        }

    }

    class Tuple15<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        private T5 element5;

        public T5 getElement5() {
            return this.element5;
        }

        public void setElement5(T5 value) {
            this.element5 = value;
        }


        private T6 element6;

        public T6 getElement6() {
            return this.element6;
        }

        public void setElement6(T6 value) {
            this.element6 = value;
        }


        private T7 element7;

        public T7 getElement7() {
            return this.element7;
        }

        public void setElement7(T7 value) {
            this.element7 = value;
        }


        private T8 element8;

        public T8 getElement8() {
            return this.element8;
        }

        public void setElement8(T8 value) {
            this.element8 = value;
        }


        private T9 element9;

        public T9 getElement9() {
            return this.element9;
        }

        public void setElement9(T9 value) {
            this.element9 = value;
        }


        private T10 element10;

        public T10 getElement10() {
            return this.element10;
        }

        public void setElement10(T10 value) {
            this.element10 = value;
        }


        private T11 element11;

        public T11 getElement11() {
            return this.element11;
        }

        public void setElement11(T11 value) {
            this.element11 = value;
        }


        private T12 element12;

        public T12 getElement12() {
            return this.element12;
        }

        public void setElement12(T12 value) {
            this.element12 = value;
        }


        private T13 element13;

        public T13 getElement13() {
            return this.element13;
        }

        public void setElement13(T13 value) {
            this.element13 = value;
        }


        private T14 element14;

        public T14 getElement14() {
            return this.element14;
        }

        public void setElement14(T14 value) {
            this.element14 = value;
        }


        public Tuple15() {
        }


        public Tuple15(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4,
                T5 _5,
                T6 _6,
                T7 _7,
                T8 _8,
                T9 _9,
                T10 _10,
                T11 _11,
                T12 _12,
                T13 _13,
                T14 _14
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ","
                    + (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5) + ","
                    + (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6) + ","
                    + (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7) + ","
                    + (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8) + ","
                    + (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9) + ","
                    + (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10) + ","
                    + (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11) + ","
                    + (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12) + ","
                    + (this.element13 instanceof String ? "\"" + this.element13 + "\"" : this.element13) + ","
                    + (this.element14 instanceof String ? "\"" + this.element14 + "\"" : this.element14) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple15<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> t = (Tuple15<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            if (!getElement4().equals(t.getElement4())) return false;
            if (!getElement5().equals(t.getElement5())) return false;
            if (!getElement6().equals(t.getElement6())) return false;
            if (!getElement7().equals(t.getElement7())) return false;
            if (!getElement8().equals(t.getElement8())) return false;
            if (!getElement9().equals(t.getElement9())) return false;
            if (!getElement10().equals(t.getElement10())) return false;
            if (!getElement11().equals(t.getElement11())) return false;
            if (!getElement12().equals(t.getElement12())) return false;
            if (!getElement13().equals(t.getElement13())) return false;
            return getElement14().equals(t.getElement14());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            result = 31 * result + getElement5().hashCode();
            result = 31 * result + getElement6().hashCode();
            result = 31 * result + getElement7().hashCode();
            result = 31 * result + getElement8().hashCode();
            result = 31 * result + getElement9().hashCode();
            result = 31 * result + getElement10().hashCode();
            result = 31 * result + getElement11().hashCode();
            result = 31 * result + getElement12().hashCode();
            result = 31 * result + getElement13().hashCode();
            result = 31 * result + getElement14().hashCode();
            return result;
        }

        public <X> Tuple16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, X> add(X x) {
            return new Tuple16<>(getElement0(), getElement1(), getElement2(), getElement3(), getElement4(), getElement5(), getElement6(), getElement7(), getElement8(), getElement9(), getElement10(), getElement11(), getElement12(), getElement13(), getElement14(), x);
        }

    }

    class Tuple16<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        private T5 element5;

        public T5 getElement5() {
            return this.element5;
        }

        public void setElement5(T5 value) {
            this.element5 = value;
        }


        private T6 element6;

        public T6 getElement6() {
            return this.element6;
        }

        public void setElement6(T6 value) {
            this.element6 = value;
        }


        private T7 element7;

        public T7 getElement7() {
            return this.element7;
        }

        public void setElement7(T7 value) {
            this.element7 = value;
        }


        private T8 element8;

        public T8 getElement8() {
            return this.element8;
        }

        public void setElement8(T8 value) {
            this.element8 = value;
        }


        private T9 element9;

        public T9 getElement9() {
            return this.element9;
        }

        public void setElement9(T9 value) {
            this.element9 = value;
        }


        private T10 element10;

        public T10 getElement10() {
            return this.element10;
        }

        public void setElement10(T10 value) {
            this.element10 = value;
        }


        private T11 element11;

        public T11 getElement11() {
            return this.element11;
        }

        public void setElement11(T11 value) {
            this.element11 = value;
        }


        private T12 element12;

        public T12 getElement12() {
            return this.element12;
        }

        public void setElement12(T12 value) {
            this.element12 = value;
        }


        private T13 element13;

        public T13 getElement13() {
            return this.element13;
        }

        public void setElement13(T13 value) {
            this.element13 = value;
        }


        private T14 element14;

        public T14 getElement14() {
            return this.element14;
        }

        public void setElement14(T14 value) {
            this.element14 = value;
        }


        private T15 element15;

        public T15 getElement15() {
            return this.element15;
        }

        public void setElement15(T15 value) {
            this.element15 = value;
        }


        public Tuple16() {
        }


        public Tuple16(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4,
                T5 _5,
                T6 _6,
                T7 _7,
                T8 _8,
                T9 _9,
                T10 _10,
                T11 _11,
                T12 _12,
                T13 _13,
                T14 _14,
                T15 _15
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ","
                    + (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5) + ","
                    + (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6) + ","
                    + (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7) + ","
                    + (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8) + ","
                    + (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9) + ","
                    + (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10) + ","
                    + (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11) + ","
                    + (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12) + ","
                    + (this.element13 instanceof String ? "\"" + this.element13 + "\"" : this.element13) + ","
                    + (this.element14 instanceof String ? "\"" + this.element14 + "\"" : this.element14) + ","
                    + (this.element15 instanceof String ? "\"" + this.element15 + "\"" : this.element15) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple16<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> t = (Tuple16<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            if (!getElement4().equals(t.getElement4())) return false;
            if (!getElement5().equals(t.getElement5())) return false;
            if (!getElement6().equals(t.getElement6())) return false;
            if (!getElement7().equals(t.getElement7())) return false;
            if (!getElement8().equals(t.getElement8())) return false;
            if (!getElement9().equals(t.getElement9())) return false;
            if (!getElement10().equals(t.getElement10())) return false;
            if (!getElement11().equals(t.getElement11())) return false;
            if (!getElement12().equals(t.getElement12())) return false;
            if (!getElement13().equals(t.getElement13())) return false;
            if (!getElement14().equals(t.getElement14())) return false;
            return getElement15().equals(t.getElement15());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            result = 31 * result + getElement5().hashCode();
            result = 31 * result + getElement6().hashCode();
            result = 31 * result + getElement7().hashCode();
            result = 31 * result + getElement8().hashCode();
            result = 31 * result + getElement9().hashCode();
            result = 31 * result + getElement10().hashCode();
            result = 31 * result + getElement11().hashCode();
            result = 31 * result + getElement12().hashCode();
            result = 31 * result + getElement13().hashCode();
            result = 31 * result + getElement14().hashCode();
            result = 31 * result + getElement15().hashCode();
            return result;
        }

        public <X> Tuple17<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, X> add(X x) {
            return new Tuple17<>(getElement0(), getElement1(), getElement2(), getElement3(), getElement4(), getElement5(), getElement6(), getElement7(), getElement8(), getElement9(), getElement10(), getElement11(), getElement12(), getElement13(), getElement14(), getElement15(), x);
        }

    }

    class Tuple17<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        private T5 element5;

        public T5 getElement5() {
            return this.element5;
        }

        public void setElement5(T5 value) {
            this.element5 = value;
        }


        private T6 element6;

        public T6 getElement6() {
            return this.element6;
        }

        public void setElement6(T6 value) {
            this.element6 = value;
        }


        private T7 element7;

        public T7 getElement7() {
            return this.element7;
        }

        public void setElement7(T7 value) {
            this.element7 = value;
        }


        private T8 element8;

        public T8 getElement8() {
            return this.element8;
        }

        public void setElement8(T8 value) {
            this.element8 = value;
        }


        private T9 element9;

        public T9 getElement9() {
            return this.element9;
        }

        public void setElement9(T9 value) {
            this.element9 = value;
        }


        private T10 element10;

        public T10 getElement10() {
            return this.element10;
        }

        public void setElement10(T10 value) {
            this.element10 = value;
        }


        private T11 element11;

        public T11 getElement11() {
            return this.element11;
        }

        public void setElement11(T11 value) {
            this.element11 = value;
        }


        private T12 element12;

        public T12 getElement12() {
            return this.element12;
        }

        public void setElement12(T12 value) {
            this.element12 = value;
        }


        private T13 element13;

        public T13 getElement13() {
            return this.element13;
        }

        public void setElement13(T13 value) {
            this.element13 = value;
        }


        private T14 element14;

        public T14 getElement14() {
            return this.element14;
        }

        public void setElement14(T14 value) {
            this.element14 = value;
        }


        private T15 element15;

        public T15 getElement15() {
            return this.element15;
        }

        public void setElement15(T15 value) {
            this.element15 = value;
        }


        private T16 element16;

        public T16 getElement16() {
            return this.element16;
        }

        public void setElement16(T16 value) {
            this.element16 = value;
        }


        public Tuple17() {
        }


        public Tuple17(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4,
                T5 _5,
                T6 _6,
                T7 _7,
                T8 _8,
                T9 _9,
                T10 _10,
                T11 _11,
                T12 _12,
                T13 _13,
                T14 _14,
                T15 _15,
                T16 _16
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ","
                    + (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5) + ","
                    + (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6) + ","
                    + (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7) + ","
                    + (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8) + ","
                    + (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9) + ","
                    + (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10) + ","
                    + (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11) + ","
                    + (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12) + ","
                    + (this.element13 instanceof String ? "\"" + this.element13 + "\"" : this.element13) + ","
                    + (this.element14 instanceof String ? "\"" + this.element14 + "\"" : this.element14) + ","
                    + (this.element15 instanceof String ? "\"" + this.element15 + "\"" : this.element15) + ","
                    + (this.element16 instanceof String ? "\"" + this.element16 + "\"" : this.element16) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple17<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> t = (Tuple17<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            if (!getElement4().equals(t.getElement4())) return false;
            if (!getElement5().equals(t.getElement5())) return false;
            if (!getElement6().equals(t.getElement6())) return false;
            if (!getElement7().equals(t.getElement7())) return false;
            if (!getElement8().equals(t.getElement8())) return false;
            if (!getElement9().equals(t.getElement9())) return false;
            if (!getElement10().equals(t.getElement10())) return false;
            if (!getElement11().equals(t.getElement11())) return false;
            if (!getElement12().equals(t.getElement12())) return false;
            if (!getElement13().equals(t.getElement13())) return false;
            if (!getElement14().equals(t.getElement14())) return false;
            if (!getElement15().equals(t.getElement15())) return false;
            return getElement16().equals(t.getElement16());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            result = 31 * result + getElement5().hashCode();
            result = 31 * result + getElement6().hashCode();
            result = 31 * result + getElement7().hashCode();
            result = 31 * result + getElement8().hashCode();
            result = 31 * result + getElement9().hashCode();
            result = 31 * result + getElement10().hashCode();
            result = 31 * result + getElement11().hashCode();
            result = 31 * result + getElement12().hashCode();
            result = 31 * result + getElement13().hashCode();
            result = 31 * result + getElement14().hashCode();
            result = 31 * result + getElement15().hashCode();
            result = 31 * result + getElement16().hashCode();
            return result;
        }

        public <X> Tuple18<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, X> add(X x) {
            return new Tuple18<>(getElement0(), getElement1(), getElement2(), getElement3(), getElement4(), getElement5(), getElement6(), getElement7(), getElement8(), getElement9(), getElement10(), getElement11(), getElement12(), getElement13(), getElement14(), getElement15(), getElement16(), x);
        }

    }

    class Tuple18<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        private T5 element5;

        public T5 getElement5() {
            return this.element5;
        }

        public void setElement5(T5 value) {
            this.element5 = value;
        }


        private T6 element6;

        public T6 getElement6() {
            return this.element6;
        }

        public void setElement6(T6 value) {
            this.element6 = value;
        }


        private T7 element7;

        public T7 getElement7() {
            return this.element7;
        }

        public void setElement7(T7 value) {
            this.element7 = value;
        }


        private T8 element8;

        public T8 getElement8() {
            return this.element8;
        }

        public void setElement8(T8 value) {
            this.element8 = value;
        }


        private T9 element9;

        public T9 getElement9() {
            return this.element9;
        }

        public void setElement9(T9 value) {
            this.element9 = value;
        }


        private T10 element10;

        public T10 getElement10() {
            return this.element10;
        }

        public void setElement10(T10 value) {
            this.element10 = value;
        }


        private T11 element11;

        public T11 getElement11() {
            return this.element11;
        }

        public void setElement11(T11 value) {
            this.element11 = value;
        }


        private T12 element12;

        public T12 getElement12() {
            return this.element12;
        }

        public void setElement12(T12 value) {
            this.element12 = value;
        }


        private T13 element13;

        public T13 getElement13() {
            return this.element13;
        }

        public void setElement13(T13 value) {
            this.element13 = value;
        }


        private T14 element14;

        public T14 getElement14() {
            return this.element14;
        }

        public void setElement14(T14 value) {
            this.element14 = value;
        }


        private T15 element15;

        public T15 getElement15() {
            return this.element15;
        }

        public void setElement15(T15 value) {
            this.element15 = value;
        }


        private T16 element16;

        public T16 getElement16() {
            return this.element16;
        }

        public void setElement16(T16 value) {
            this.element16 = value;
        }


        private T17 element17;

        public T17 getElement17() {
            return this.element17;
        }

        public void setElement17(T17 value) {
            this.element17 = value;
        }


        public Tuple18() {
        }


        public Tuple18(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4,
                T5 _5,
                T6 _6,
                T7 _7,
                T8 _8,
                T9 _9,
                T10 _10,
                T11 _11,
                T12 _12,
                T13 _13,
                T14 _14,
                T15 _15,
                T16 _16,
                T17 _17
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ","
                    + (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5) + ","
                    + (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6) + ","
                    + (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7) + ","
                    + (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8) + ","
                    + (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9) + ","
                    + (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10) + ","
                    + (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11) + ","
                    + (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12) + ","
                    + (this.element13 instanceof String ? "\"" + this.element13 + "\"" : this.element13) + ","
                    + (this.element14 instanceof String ? "\"" + this.element14 + "\"" : this.element14) + ","
                    + (this.element15 instanceof String ? "\"" + this.element15 + "\"" : this.element15) + ","
                    + (this.element16 instanceof String ? "\"" + this.element16 + "\"" : this.element16) + ","
                    + (this.element17 instanceof String ? "\"" + this.element17 + "\"" : this.element17) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple18<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> t = (Tuple18<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            if (!getElement4().equals(t.getElement4())) return false;
            if (!getElement5().equals(t.getElement5())) return false;
            if (!getElement6().equals(t.getElement6())) return false;
            if (!getElement7().equals(t.getElement7())) return false;
            if (!getElement8().equals(t.getElement8())) return false;
            if (!getElement9().equals(t.getElement9())) return false;
            if (!getElement10().equals(t.getElement10())) return false;
            if (!getElement11().equals(t.getElement11())) return false;
            if (!getElement12().equals(t.getElement12())) return false;
            if (!getElement13().equals(t.getElement13())) return false;
            if (!getElement14().equals(t.getElement14())) return false;
            if (!getElement15().equals(t.getElement15())) return false;
            if (!getElement16().equals(t.getElement16())) return false;
            return getElement17().equals(t.getElement17());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            result = 31 * result + getElement5().hashCode();
            result = 31 * result + getElement6().hashCode();
            result = 31 * result + getElement7().hashCode();
            result = 31 * result + getElement8().hashCode();
            result = 31 * result + getElement9().hashCode();
            result = 31 * result + getElement10().hashCode();
            result = 31 * result + getElement11().hashCode();
            result = 31 * result + getElement12().hashCode();
            result = 31 * result + getElement13().hashCode();
            result = 31 * result + getElement14().hashCode();
            result = 31 * result + getElement15().hashCode();
            result = 31 * result + getElement16().hashCode();
            result = 31 * result + getElement17().hashCode();
            return result;
        }

        public <X> Tuple19<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, X> add(X x) {
            return new Tuple19<>(getElement0(), getElement1(), getElement2(), getElement3(), getElement4(), getElement5(), getElement6(), getElement7(), getElement8(), getElement9(), getElement10(), getElement11(), getElement12(), getElement13(), getElement14(), getElement15(), getElement16(), getElement17(), x);
        }

    }

    class Tuple19<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        private T5 element5;

        public T5 getElement5() {
            return this.element5;
        }

        public void setElement5(T5 value) {
            this.element5 = value;
        }


        private T6 element6;

        public T6 getElement6() {
            return this.element6;
        }

        public void setElement6(T6 value) {
            this.element6 = value;
        }


        private T7 element7;

        public T7 getElement7() {
            return this.element7;
        }

        public void setElement7(T7 value) {
            this.element7 = value;
        }


        private T8 element8;

        public T8 getElement8() {
            return this.element8;
        }

        public void setElement8(T8 value) {
            this.element8 = value;
        }


        private T9 element9;

        public T9 getElement9() {
            return this.element9;
        }

        public void setElement9(T9 value) {
            this.element9 = value;
        }


        private T10 element10;

        public T10 getElement10() {
            return this.element10;
        }

        public void setElement10(T10 value) {
            this.element10 = value;
        }


        private T11 element11;

        public T11 getElement11() {
            return this.element11;
        }

        public void setElement11(T11 value) {
            this.element11 = value;
        }


        private T12 element12;

        public T12 getElement12() {
            return this.element12;
        }

        public void setElement12(T12 value) {
            this.element12 = value;
        }


        private T13 element13;

        public T13 getElement13() {
            return this.element13;
        }

        public void setElement13(T13 value) {
            this.element13 = value;
        }


        private T14 element14;

        public T14 getElement14() {
            return this.element14;
        }

        public void setElement14(T14 value) {
            this.element14 = value;
        }


        private T15 element15;

        public T15 getElement15() {
            return this.element15;
        }

        public void setElement15(T15 value) {
            this.element15 = value;
        }


        private T16 element16;

        public T16 getElement16() {
            return this.element16;
        }

        public void setElement16(T16 value) {
            this.element16 = value;
        }


        private T17 element17;

        public T17 getElement17() {
            return this.element17;
        }

        public void setElement17(T17 value) {
            this.element17 = value;
        }


        private T18 element18;

        public T18 getElement18() {
            return this.element18;
        }

        public void setElement18(T18 value) {
            this.element18 = value;
        }


        public Tuple19() {
        }


        public Tuple19(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4,
                T5 _5,
                T6 _6,
                T7 _7,
                T8 _8,
                T9 _9,
                T10 _10,
                T11 _11,
                T12 _12,
                T13 _13,
                T14 _14,
                T15 _15,
                T16 _16,
                T17 _17,
                T18 _18
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ","
                    + (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5) + ","
                    + (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6) + ","
                    + (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7) + ","
                    + (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8) + ","
                    + (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9) + ","
                    + (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10) + ","
                    + (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11) + ","
                    + (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12) + ","
                    + (this.element13 instanceof String ? "\"" + this.element13 + "\"" : this.element13) + ","
                    + (this.element14 instanceof String ? "\"" + this.element14 + "\"" : this.element14) + ","
                    + (this.element15 instanceof String ? "\"" + this.element15 + "\"" : this.element15) + ","
                    + (this.element16 instanceof String ? "\"" + this.element16 + "\"" : this.element16) + ","
                    + (this.element17 instanceof String ? "\"" + this.element17 + "\"" : this.element17) + ","
                    + (this.element18 instanceof String ? "\"" + this.element18 + "\"" : this.element18) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple19<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> t = (Tuple19<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            if (!getElement4().equals(t.getElement4())) return false;
            if (!getElement5().equals(t.getElement5())) return false;
            if (!getElement6().equals(t.getElement6())) return false;
            if (!getElement7().equals(t.getElement7())) return false;
            if (!getElement8().equals(t.getElement8())) return false;
            if (!getElement9().equals(t.getElement9())) return false;
            if (!getElement10().equals(t.getElement10())) return false;
            if (!getElement11().equals(t.getElement11())) return false;
            if (!getElement12().equals(t.getElement12())) return false;
            if (!getElement13().equals(t.getElement13())) return false;
            if (!getElement14().equals(t.getElement14())) return false;
            if (!getElement15().equals(t.getElement15())) return false;
            if (!getElement16().equals(t.getElement16())) return false;
            if (!getElement17().equals(t.getElement17())) return false;
            return getElement18().equals(t.getElement18());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            result = 31 * result + getElement5().hashCode();
            result = 31 * result + getElement6().hashCode();
            result = 31 * result + getElement7().hashCode();
            result = 31 * result + getElement8().hashCode();
            result = 31 * result + getElement9().hashCode();
            result = 31 * result + getElement10().hashCode();
            result = 31 * result + getElement11().hashCode();
            result = 31 * result + getElement12().hashCode();
            result = 31 * result + getElement13().hashCode();
            result = 31 * result + getElement14().hashCode();
            result = 31 * result + getElement15().hashCode();
            result = 31 * result + getElement16().hashCode();
            result = 31 * result + getElement17().hashCode();
            result = 31 * result + getElement18().hashCode();
            return result;
        }

        public <X> Tuple20<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, X> add(X x) {
            return new Tuple20<>(getElement0(), getElement1(), getElement2(), getElement3(), getElement4(), getElement5(), getElement6(), getElement7(), getElement8(), getElement9(), getElement10(), getElement11(), getElement12(), getElement13(), getElement14(), getElement15(), getElement16(), getElement17(), getElement18(), x);
        }

    }

    class Tuple20<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> implements Tuple {


        private T0 element0;

        public T0 getElement0() {
            return this.element0;
        }

        public void setElement0(T0 value) {
            this.element0 = value;
        }


        private T1 element1;

        public T1 getElement1() {
            return this.element1;
        }

        public void setElement1(T1 value) {
            this.element1 = value;
        }


        private T2 element2;

        public T2 getElement2() {
            return this.element2;
        }

        public void setElement2(T2 value) {
            this.element2 = value;
        }


        private T3 element3;

        public T3 getElement3() {
            return this.element3;
        }

        public void setElement3(T3 value) {
            this.element3 = value;
        }


        private T4 element4;

        public T4 getElement4() {
            return this.element4;
        }

        public void setElement4(T4 value) {
            this.element4 = value;
        }


        private T5 element5;

        public T5 getElement5() {
            return this.element5;
        }

        public void setElement5(T5 value) {
            this.element5 = value;
        }


        private T6 element6;

        public T6 getElement6() {
            return this.element6;
        }

        public void setElement6(T6 value) {
            this.element6 = value;
        }


        private T7 element7;

        public T7 getElement7() {
            return this.element7;
        }

        public void setElement7(T7 value) {
            this.element7 = value;
        }


        private T8 element8;

        public T8 getElement8() {
            return this.element8;
        }

        public void setElement8(T8 value) {
            this.element8 = value;
        }


        private T9 element9;

        public T9 getElement9() {
            return this.element9;
        }

        public void setElement9(T9 value) {
            this.element9 = value;
        }


        private T10 element10;

        public T10 getElement10() {
            return this.element10;
        }

        public void setElement10(T10 value) {
            this.element10 = value;
        }


        private T11 element11;

        public T11 getElement11() {
            return this.element11;
        }

        public void setElement11(T11 value) {
            this.element11 = value;
        }


        private T12 element12;

        public T12 getElement12() {
            return this.element12;
        }

        public void setElement12(T12 value) {
            this.element12 = value;
        }


        private T13 element13;

        public T13 getElement13() {
            return this.element13;
        }

        public void setElement13(T13 value) {
            this.element13 = value;
        }


        private T14 element14;

        public T14 getElement14() {
            return this.element14;
        }

        public void setElement14(T14 value) {
            this.element14 = value;
        }


        private T15 element15;

        public T15 getElement15() {
            return this.element15;
        }

        public void setElement15(T15 value) {
            this.element15 = value;
        }


        private T16 element16;

        public T16 getElement16() {
            return this.element16;
        }

        public void setElement16(T16 value) {
            this.element16 = value;
        }


        private T17 element17;

        public T17 getElement17() {
            return this.element17;
        }

        public void setElement17(T17 value) {
            this.element17 = value;
        }


        private T18 element18;

        public T18 getElement18() {
            return this.element18;
        }

        public void setElement18(T18 value) {
            this.element18 = value;
        }


        private T19 element19;

        public T19 getElement19() {
            return this.element19;
        }

        public void setElement19(T19 value) {
            this.element19 = value;
        }


        public Tuple20() {
        }


        public Tuple20(
                T0 _0,
                T1 _1,
                T2 _2,
                T3 _3,
                T4 _4,
                T5 _5,
                T6 _6,
                T7 _7,
                T8 _8,
                T9 _9,
                T10 _10,
                T11 _11,
                T12 _12,
                T13 _13,
                T14 _14,
                T15 _15,
                T16 _16,
                T17 _17,
                T18 _18,
                T19 _19
        ) {
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
            return "(" + (this.element0 instanceof String ? "\"" + this.element0 + "\"" : this.element0) + ","
                    + (this.element1 instanceof String ? "\"" + this.element1 + "\"" : this.element1) + ","
                    + (this.element2 instanceof String ? "\"" + this.element2 + "\"" : this.element2) + ","
                    + (this.element3 instanceof String ? "\"" + this.element3 + "\"" : this.element3) + ","
                    + (this.element4 instanceof String ? "\"" + this.element4 + "\"" : this.element4) + ","
                    + (this.element5 instanceof String ? "\"" + this.element5 + "\"" : this.element5) + ","
                    + (this.element6 instanceof String ? "\"" + this.element6 + "\"" : this.element6) + ","
                    + (this.element7 instanceof String ? "\"" + this.element7 + "\"" : this.element7) + ","
                    + (this.element8 instanceof String ? "\"" + this.element8 + "\"" : this.element8) + ","
                    + (this.element9 instanceof String ? "\"" + this.element9 + "\"" : this.element9) + ","
                    + (this.element10 instanceof String ? "\"" + this.element10 + "\"" : this.element10) + ","
                    + (this.element11 instanceof String ? "\"" + this.element11 + "\"" : this.element11) + ","
                    + (this.element12 instanceof String ? "\"" + this.element12 + "\"" : this.element12) + ","
                    + (this.element13 instanceof String ? "\"" + this.element13 + "\"" : this.element13) + ","
                    + (this.element14 instanceof String ? "\"" + this.element14 + "\"" : this.element14) + ","
                    + (this.element15 instanceof String ? "\"" + this.element15 + "\"" : this.element15) + ","
                    + (this.element16 instanceof String ? "\"" + this.element16 + "\"" : this.element16) + ","
                    + (this.element17 instanceof String ? "\"" + this.element17 + "\"" : this.element17) + ","
                    + (this.element18 instanceof String ? "\"" + this.element18 + "\"" : this.element18) + ","
                    + (this.element19 instanceof String ? "\"" + this.element19 + "\"" : this.element19) + ")";
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple20<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?> t = (Tuple20<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) o;

            if (!getElement0().equals(t.getElement0())) return false;
            if (!getElement1().equals(t.getElement1())) return false;
            if (!getElement2().equals(t.getElement2())) return false;
            if (!getElement3().equals(t.getElement3())) return false;
            if (!getElement4().equals(t.getElement4())) return false;
            if (!getElement5().equals(t.getElement5())) return false;
            if (!getElement6().equals(t.getElement6())) return false;
            if (!getElement7().equals(t.getElement7())) return false;
            if (!getElement8().equals(t.getElement8())) return false;
            if (!getElement9().equals(t.getElement9())) return false;
            if (!getElement10().equals(t.getElement10())) return false;
            if (!getElement11().equals(t.getElement11())) return false;
            if (!getElement12().equals(t.getElement12())) return false;
            if (!getElement13().equals(t.getElement13())) return false;
            if (!getElement14().equals(t.getElement14())) return false;
            if (!getElement15().equals(t.getElement15())) return false;
            if (!getElement16().equals(t.getElement16())) return false;
            if (!getElement17().equals(t.getElement17())) return false;
            if (!getElement18().equals(t.getElement18())) return false;
            return getElement19().equals(t.getElement19());
        }


        @Override
        public int hashCode() {
            int result = getElement0().hashCode();
            result = 31 * result + getElement1().hashCode();
            result = 31 * result + getElement2().hashCode();
            result = 31 * result + getElement3().hashCode();
            result = 31 * result + getElement4().hashCode();
            result = 31 * result + getElement5().hashCode();
            result = 31 * result + getElement6().hashCode();
            result = 31 * result + getElement7().hashCode();
            result = 31 * result + getElement8().hashCode();
            result = 31 * result + getElement9().hashCode();
            result = 31 * result + getElement10().hashCode();
            result = 31 * result + getElement11().hashCode();
            result = 31 * result + getElement12().hashCode();
            result = 31 * result + getElement13().hashCode();
            result = 31 * result + getElement14().hashCode();
            result = 31 * result + getElement15().hashCode();
            result = 31 * result + getElement16().hashCode();
            result = 31 * result + getElement17().hashCode();
            result = 31 * result + getElement18().hashCode();
            result = 31 * result + getElement19().hashCode();
            return result;
        }

    }


    //Use this main to generate inner classes source from Tuple2 to Tuple20
    static void main(String[] argv) {
        for (int n = 2; n <= 20; n++) {
            //header
            StringBuilder sb = new StringBuilder("class Tuple");
            sb.append(n).append("<");
            for (int i = 0; i < n; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append("T").append(i);
            }
            sb.append("> implements Tuple {\n\n");

            for (int i = 0; i < n; i++) {
                //properties
                sb.append("\n");
                sb.append("private T").append(i).append(" element").append(i).append(";\n\n");

                sb.append("public T").append(i).append(" getElement").append(i).append("(){\n");
                sb.append("return this.element").append(i).append(";\n");
                sb.append("}\n\n");

                sb.append("public void setElement").append(i).append("(T").append(i).append(" value){\n");
                sb.append("this.element").append(i).append(" = value;\n");
                sb.append("}\n\n");
            }

            //default ctor
            sb.append("\n");
            sb.append("public Tuple").append(n).append("(){\n");
            sb.append("}\n\n");

            //ctor
            sb.append("\n");
            sb.append("public Tuple").append(n).append("(\n");
            for (int i = 0; i < n; i++) {
                //ctor params
                if (i != 0) {
                    sb.append(",\n");
                }
                sb.append("T").append(i).append(" _").append(i);
            }
            sb.append("\n){\n");
            for (int i = 0; i < n; i++) {
                sb.append("this.element").append(i).append(" = _").append(i).append(";\n");
            }
            sb.append("}\n\n");

            //getLength()
            sb.append("@Override\n");
            sb.append("public int getLength() {\n");
            sb.append("return ").append(n).append(";\n");
            sb.append("}\n\n");

            //toString()
            sb.append("@Override\n");
            sb.append("public String toString() {\n");
            sb.append("return \"(\"");
            for (int i = 0; i < n; i++) {
                if (i != 0) {
                    sb.append("+ \",\"\n");
                }
                sb.append("+ (this.element").append(i).append(" instanceof String ? \"\\\"\" + this.element").append(i)
                        .append(" + \"\\\"\" : this.element").append(i).append(")");
            }
            sb.append(" + \")\";\n");

            sb.append("}\n\n\n");

            //equals()
            sb.append("@Override\n");
            sb.append("public boolean equals(Object o) {\n");
            sb.append("if (this == o) return true;\n");
            sb.append("if (o == null || getClass() != o.getClass()) return false;\n");
            sb.append("Tuple").append(n).append("<");
            for (int i = 0; i < n; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append("?");
            }
            sb.append("> t = (Tuple").append(n).append("<");
            for (int i = 0; i < n; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append("?");
            }
            sb.append(">) o;\n\n");
            for (int i = 0; i < n - 1; i++) {
                sb.append("if(!getElement").append(i).append("().equals(t.getElement").append(i).append("()))")
                        .append("return false;\n");
            }
            int x = n - 1;
            sb.append("return getElement").append(x).append("().equals(t.getElement").append(x).append("());\n");
            sb.append("}\n\n\n");

            //hashCode()
            sb.append("@Override\n");
            sb.append("public int hashCode() {\n");
            sb.append("int result = getElement0().hashCode();\n");
            for (int i = 1; i < n; i++) {
                sb.append("result = 31 * result + getElement").append(i).append("().hashCode();\n");
            }
            sb.append("return result;\n");
            sb.append("}\n\n");

            if (n != 20) {
                //add()
                sb.append("public <X> Tuple").append(n + 1).append("<");
                for (int i = 0; i < n; i++) {
                    if (i != 0) {
                        sb.append(", ");
                    }
                    sb.append("T").append(i);
                }
                sb.append(", X> add(X x){\n");
                sb.append("return new Tuple").append(n + 1).append("<>(");
                for (int i = 0; i < n; i++) {
                    if (i != 0) {
                        sb.append(", ");
                    }
                    sb.append("getElement").append(i).append("()");
                }
                sb.append(", x);\n");
                sb.append("}\n\n");
            }
            //End class
            sb.append("}\n");

            System.out.println(sb);
        }
    }

}
