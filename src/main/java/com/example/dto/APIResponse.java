        package com.example.dto;

        import lombok.AllArgsConstructor;
        import lombok.Data;

        @Data
        //@AllArgsConstructor
        public class APIResponse<T> {
            private long totalElements;
            private T data;

            public APIResponse(long totalElements, T data) {
                this.totalElements = totalElements;
                this.data = data;
            }

            public long getTotalElements() {
                return totalElements;
            }

            public void setTotalElements(long totalElements) {
                this.totalElements = totalElements;
            }

            public T getData() {
                return data;
            }

            public void setData(T data) {
                this.data = data;
            }
        }

