<template>
  <v-card class="mb-12" :loading="loading">
    <v-card-text>
      <v-row dense align="center" class="mb-3">
        <v-col cols="12" sm="3">
          <v-select
            v-model="searchField"
            :items="searchFields"
            item-text="label"
            item-value="value"
            label="搜索范围"
            dense
            outlined
            hide-details
          />
        </v-col>
        <v-col cols="12" sm="7">
          <v-text-field
            v-model="searchText"
            label="搜索图书"
            placeholder="输入书名、作者、出版社或 ISBN"
            prepend-inner-icon="mdi-magnify"
            clearable
            dense
            outlined
            hide-details
            @keydown.enter.prevent
          />
        </v-col>
        <v-col cols="12" sm="2">
          <v-btn block text color="blue darken-1" @click="clearSearch">
            清空
          </v-btn>
        </v-col>
      </v-row>

      <div class="caption grey--text mb-2">
        共 {{ displayedBooks.length }} 本匹配图书
      </div>

      <v-list shaped>
        <v-list-item-group
          v-model="selectedNewBook"
          v-if="displayedBooks.length"
        >
          <v-virtual-scroll
            :bench="displayedBooks.length"
            :items="displayedBooks"
            item-height="70"
            height="300"
          >
            <template v-slot="{ item }">
              <v-list-item
                :key="item.id"
                :value="item"
                active-class="green--text text--accent-4"
              >
                <template v-slot:default="{ active }">
                  <v-list-item-content>
                    <v-list-item-title
                      class="green--text"
                      v-text="item.name"
                    ></v-list-item-title>
                    <v-list-item-subtitle
                      v-text="bookSubtitle(item)"
                    ></v-list-item-subtitle>
                    <v-list-item-subtitle
                      v-if="active"
                      v-text="item.summary"
                      class="text-truncate"
                    ></v-list-item-subtitle>
                  </v-list-item-content>
                  <v-list-item-action>
                    <v-checkbox
                      :input-value="active"
                      color="dark-green"
                    ></v-checkbox>
                  </v-list-item-action>
                </template>
              </v-list-item>
            </template>
          </v-virtual-scroll>
        </v-list-item-group>
        <v-alert v-else dense outlined type="info" class="mb-0">
          没有找到匹配的图书，请换一个关键词试试。
        </v-alert>
      </v-list>
    </v-card-text>
    <v-card-actions>
      <v-btn
        color="green"
        text
        @click="searchInventory"
        :disabled="!selectedNewBook"
      >
        继续
      </v-btn>
      <span class="ml-5" v-if="selectedNewBook && !$vuetify.breakpoint.mobile">
        订单：
      </span>
      <span class="ml-2 text-truncate">{{ orderName }}</span>
    </v-card-actions>
  </v-card>
</template>

<script>
import * as bookService from "@/service/book";

export default {
  name: "ChooseBook",
  data() {
    return {
      searchText: "",
      searchField: "all",
      searchFields: [
        { value: "all", label: "全部字段" },
        { value: "name", label: "书名" },
        { value: "author", label: "作者" },
        { value: "publication", label: "出版社" },
        { value: "isbn", label: "ISBN" }
      ],
      books: [],
      selectedNewBook: null,
      loading: false
    };
  },
  methods: {
    searchInventory() {
      this.$emit("searchInventory", this.selectedNewBook);
    },
    searchBooks() {
      this.loading = true;
      bookService
        .findAllBooks()
        .then(data => {
          this.loading = false;
          this.books = data;
        })
        .catch(err => {
          console.log("Error", err);
          this.loading = false;
          this.$store.commit("setErrorMessage", err.error_description);
        });
    },
    clearSearch() {
      this.searchText = "";
      this.searchField = "all";
    },
    bookSubtitle(book) {
      return (
        "作者：" +
        book.author +
        "，分类：" +
        (book.category ? book.category.name : "-") +
        "，出版社：" +
        book.publication +
        "，出版年份：" +
        (book.publicationYear || "-") +
        "，ISBN：" +
        book.isbn
      );
    },
    normalize(value) {
      return String(value || "").toLowerCase();
    },
    bookMatches(book, keyword) {
      const searchableFields =
        this.searchField === "all"
          ? ["name", "author", "publication", "isbn"]
          : [this.searchField];

      return searchableFields.some(field =>
        this.normalize(book[field]).includes(keyword)
      );
    }
  },
  computed: {
    displayedBooks() {
      const keyword = this.normalize(this.searchText).trim();
      if (!keyword) {
        return this.books;
      }
      return this.books.filter(book => this.bookMatches(book, keyword));
    },
    orderName() {
      if (this.selectedNewBook != null) {
        return this.selectedNewBook.name + ",  " + this.selectedNewBook.author;
      }
      return "";
    }
  },
  watch: {
    displayedBooks() {
      if (
        this.selectedNewBook &&
        !this.displayedBooks.some(book => book.id === this.selectedNewBook.id)
      ) {
        this.selectedNewBook = null;
      }
    }
  },
  created() {
    this.searchBooks();
  }
};
</script>

<style></style>
