package it.unipr.ailab.maybe;

public interface Either3<L, C, R> {
    class Left<L, C, R> implements Either3<L, C, R>{
        private final L x;
        public Left(L x){
            this.x = x;
        }
        public L get(){
            return x;
        }
    }


    class Center<L, C, R> implements Either3<L, C, R>{
        private final C x;
        public Center(C x){
            this.x = x;
        }
        public C get(){
            return x;
        }
    }

    class Right<L, C, R> implements Either3<L, C, R>{
        private final R x;
        public Right(R x){
            this.x = x;
        }
        public R get(){
            return x;
        }
    }
}
