package it.unipr.ailab.maybe;

public interface Either<L, R> {
    class Left<L, R> implements Either<L, R>{
        private final L left;

        public Left(L left) {
            this.left = left;
        }

        public L getLeft(){
            return left;
        }
    }


    class Right<L, R> implements Either<L, R>{
        private final R right;

        public Right(R right) {
            this.right = right;
        }

        public R getRight(){
            return right;
        }
    }
}
